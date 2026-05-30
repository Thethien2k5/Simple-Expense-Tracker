package com.T2V.simple_expense_tracker.service

import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.T2V.simple_expense_tracker.domain.model.*
import com.T2V.simple_expense_tracker.domain.parser.NotificationParser
import com.T2V.simple_expense_tracker.domain.repository.BankAccountRepository
import com.T2V.simple_expense_tracker.domain.repository.RawNotificationRepository
import com.T2V.simple_expense_tracker.domain.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import kotlin.math.abs

/**
 * Dịch vụ chạy ngầm bắt các thông báo biến động số dư ngân hàng
 * và tự động cập nhật cơ sở dữ liệu Room.
 *
 * Đã nâng cấp:
 * - Chỉ nhận thông báo từ App ngân hàng (bỏ qua SMS hoàn toàn).
 * - Kiểm tra tính nhất quán số dư (Balance Consistency).
 * - Tích hợp NotificationParser 3 tầng (Regex -> ML Kit -> Thủ công).
 */
@AndroidEntryPoint
class BankNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var rawNotificationRepository: RawNotificationRepository

    @Inject
    lateinit var bankAccountRepository: BankAccountRepository

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var notificationParser: NotificationParser

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Mutex để đảm bảo chỉ có 1 coroutine cập nhật số dư tài khoản tại một thời điểm.
     * Tránh race condition khi nhiều thông báo đến gần cùng lúc (ví dụ 2 giao dịch từ TNEX
     * hoặc VCB bundled notifications).
     */
    private val accountUpdateMutex = Mutex()

    companion object {
        private const val TAG = "BankNotificationService"
        // Ngưỡng sai lệch số dư cho phép (VND) - do làm tròn
        private const val BALANCE_TOLERANCE = 1.0
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "Dịch vụ đọc thông báo đã được kết nối (onBind)")
        return super.onBind(intent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            val packageName = it.packageName
            val extras = it.notification.extras
            val title = extras.getString("android.title", "") ?: ""
            val postTime = it.postTime

            // --- Fix: Đọc nội dung với fallback đầy đủ ---
            // Android đôi khi bundled notifications khiến android.text rỗng.
            // Thứ tự ưu tiên: bigText > textLines > text > subText
            val rawText = extras.getCharSequence("android.text", "")?.toString() ?: ""
            val bigText = extras.getCharSequence("android.bigText", "")?.toString() ?: ""
            val subText = extras.getCharSequence("android.subText", "")?.toString() ?: ""
            val textLines: String = run {
                val lines = extras.getCharSequenceArray("android.textLines")
                lines?.joinToString(" ") { line -> line.toString() } ?: ""
            }

            // Chọn nội dung dài nhất và có nghĩa nhất
            val text = listOf(bigText, textLines, rawText, subText)
                .filter { s -> s.isNotBlank() }
                .maxByOrNull { s -> s.length }
                ?: ""

            Log.d(TAG, "Nhận thông báo từ: $packageName | Tiêu đề: $title")
            Log.d(TAG, "  rawText='$rawText' | bigText='${bigText.take(80)}' | textLines='${textLines.take(80)}'")
            Log.d(TAG, "  → Nội dung cuối cùng: '${text.take(120)}'")

            // Sử dụng bộ lọc mới: Blacklist SMS, Whitelist ngân hàng, loại bỏ quảng cáo
            // Nếu text vẫn rỗng sau tất cả fallback, bỏ qua hoàn toàn để không lưu raw notification rỗng
            if (text.isBlank()) {
                Log.d(TAG, "Bỏ qua thông báo rỗng từ: $packageName | Tiêu đề: $title (tất cả fields đều rỗng)")
                return@let
            }
            if (notificationParser.isBankNotification(packageName, title, text)) {
                serviceScope.launch {
                    processNotification(packageName, title, text, postTime)
                }
            }
        }
    }

    /**
     * Quy trình xử lý thông báo nâng cao:
     * 1. Nhận diện ngân hàng.
     * 2. Lưu thông báo thô (isProcessed = false).
     * 3. Phân tích 3 tầng (Regex -> ML Kit -> Thủ công).
     * 4. Kiểm tra tính nhất quán số dư.
     * 5. Lưu giao dịch.
     */
    private suspend fun processNotification(packageName: String, title: String, text: String, timestamp: Long) {
        try {
            // Bước 1: Xác định tên ngân hàng
            val bankName = notificationParser.detectBankName(packageName, title)

            // Bước 2: Lưu thông báo thô
            val rawNotification = RawNotification(
                bankName = bankName,
                fullContent = text,
                receivedAt = timestamp,
                isProcessed = false
            )
            val insertedNotificationId = rawNotificationRepository.insertNotification(rawNotification)
            Log.d(TAG, "Đã lưu thông báo thô vào Database, ID: $insertedNotificationId")

            // Bước 3: Phân tích 3 tầng
            val parseOutput = notificationParser.parseMultiTier(bankName, text, timestamp)

            when (parseOutput.result) {
                ParseResult.SUCCESS -> {
                    val parsedData = parseOutput.parsedData!!
                    Log.d(TAG, "Phân tích thành công: Bank=$bankName, Amount=${parsedData.amount}, isCredit=${parsedData.isCredit}")

                    // Bước 4: Xác định BankAccount
                    val txAmount = if (parsedData.isCredit) parsedData.amount else -parsedData.amount

                    // Fix: Bọc toàn bộ phần đọc + cập nhật số dư trong Mutex
                    // để tránh race condition khi 2 thông báo xử lý song song
                    accountUpdateMutex.withLock {
                        // Đọc lại bankAccount trong mutex để có giá trị mới nhất
                        val freshBankAccount = findBankAccount(parsedData.accountNumber, bankName)

                        if (freshBankAccount == null) {
                            // Tài khoản mới: Tạo mới và lưu giao dịch ngay
                            val colorHex = notificationParser.getBankColor(bankName)
                            val initialBalance = parsedData.balance ?: 0.0
                            val newBankAccount = BankAccount(
                                bankName = bankName,
                                accountNumber = parsedData.accountNumber,
                                iconRes = "account_balance",
                                colorHex = colorHex,
                                balance = initialBalance
                            )
                            val bankAccountId = bankAccountRepository.insertBankAccount(newBankAccount)
                            Log.d(TAG, "Tự động tạo tài khoản ngân hàng mới, ID: $bankAccountId, Số dư khởi tạo: $initialBalance")

                            // Lưu giao dịch
                            saveTransaction(insertedNotificationId, bankAccountId, txAmount, parsedData, rawNotification)
                        } else {
                            val bankAccountId = freshBankAccount.id

                            // Bước 5: Cập nhật số dư.
                            // Không tự động cộng txAmount vào số dư nếu thông báo không chứa số dư
                            // (tránh trường hợp đoán sai số dư).
                            var finalBalance = freshBankAccount.balance
                            if (parsedData.balance != null) {
                                val expectedBalance = freshBankAccount.balance + txAmount
                                val difference = abs(parsedData.balance - expectedBalance)
                                if (difference > BALANCE_TOLERANCE) {
                                    Log.w(TAG, "⚠️ Phát hiện sai lệch số dư. Dự kiến=$expectedBalance, Thông báo=${parsedData.balance}. Ép cập nhật theo thông báo.")
                                }
                                finalBalance = parsedData.balance
                            }

                            val updatedBankAccount = freshBankAccount.copy(balance = finalBalance)
                            bankAccountRepository.updateBankAccount(updatedBankAccount)
                            Log.d(TAG, "Cập nhật tài khoản, ID: $bankAccountId, Số dư mới: $finalBalance (có SD từ thông báo: ${parsedData.balance != null})")

                            // Lưu giao dịch
                            saveTransaction(insertedNotificationId, bankAccountId, txAmount, parsedData, rawNotification)
                        }
                    }
                }


                ParseResult.REJECTED -> {
                    Log.d(TAG, "Thông báo bị từ chối (nội dung trống hoặc không hợp lệ).")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi xảy ra trong quá trình xử lý thông báo: ${e.message}", e)
        }
    }

    /**
     * Tìm tài khoản ngân hàng: ưu tiên theo số tài khoản, fallback theo tên ngân hàng
     * (cho các ví điện tử như Momo không có số tài khoản).
     */
    private suspend fun findBankAccount(accountNumber: String, bankName: String): BankAccount? {
        // Nếu có số tài khoản thực tế, tìm theo số tài khoản
        if (accountNumber != "DEFAULT_ACC") {
            val account = bankAccountRepository.getBankAccountByNumber(accountNumber)
            if (account != null) return account
        }
        // Fallback: Tìm theo tên ngân hàng (dùng cho Momo, ví điện tử)
        return bankAccountRepository.getBankAccountByName(bankName)
    }

    /**
     * Lưu giao dịch và đánh dấu thông báo thô đã xử lý.
     */
    private suspend fun saveTransaction(
        notificationId: Long,
        bankAccountId: Long,
        txAmount: Double,
        parsedData: ParsedData,
        rawNotification: RawNotification
    ) {
        val transaction = Transaction(
            rawNotificationId = notificationId,
            bankAccountId = bankAccountId,
            amount = txAmount,
            counterparty = parsedData.counterparty,
            content = parsedData.content,
            timestamp = parsedData.timestamp
        )
        transactionRepository.insertTransaction(transaction)
        Log.d(TAG, "Đã lưu giao dịch tự động vào Database thành công!")

        // Cập nhật thông báo thô thành đã xử lý
        val processedNotification = rawNotification.copy(
            id = notificationId,
            isProcessed = true
        )
        rawNotificationRepository.updateNotification(processedNotification)
    }
}
