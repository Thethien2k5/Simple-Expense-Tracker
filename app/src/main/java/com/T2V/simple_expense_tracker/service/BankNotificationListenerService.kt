package com.T2V.simple_expense_tracker.service

import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.T2V.simple_expense_tracker.domain.model.BankAccount
import com.T2V.simple_expense_tracker.domain.model.RawNotification
import com.T2V.simple_expense_tracker.domain.model.Transaction
import com.T2V.simple_expense_tracker.domain.parser.NotificationParser
import com.T2V.simple_expense_tracker.domain.repository.BankAccountRepository
import com.T2V.simple_expense_tracker.domain.repository.RawNotificationRepository
import com.T2V.simple_expense_tracker.domain.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Dịch vụ chạy ngầm của hệ thống bắt các thông báo biến động số dư ngân hàng và tự động cập nhật cơ sở dữ liệu Room.
 */
@AndroidEntryPoint
class BankNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var rawNotificationRepository: RawNotificationRepository

    @Inject
    lateinit var bankAccountRepository: BankAccountRepository

    @Inject
    lateinit var transactionRepository: TransactionRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? {
        Log.d("BankNotificationService", "Dịch vụ đọc thông báo đã được kết nối (onBind)")
        return super.onBind(intent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let {
            val packageName = it.packageName
            val extras = it.notification.extras
            val title = extras.getString("android.title", "")
            val text = extras.getCharSequence("android.text", "").toString()
            val postTime = it.postTime

            Log.d("BankNotificationService", "Nhận thông báo từ: $packageName | Tiêu đề: $title | Nội dung: $text")

            // Lọc xem có phải thông báo ngân hàng hay biến động số dư không
            if (isBankOrTransactionNotification(packageName, title, text)) {
                serviceScope.launch {
                    processNotification(packageName, title, text, postTime)
                }
            }
        }
    }

    /**
     * Nhận dạng xem thông báo có phải từ ứng dụng ngân hàng hoặc chứa biến động số dư chuyển khoản hay không.
     */
    private fun isBankOrTransactionNotification(packageName: String, title: String, text: String): Boolean {
        val lowerPkg = packageName.lowercase()
        val lowerTitle = title.lowercase()
        val lowerText = text.lowercase()

        // 1. Kiểm tra package name của các ngân hàng phổ biến ở Việt Nam
        val isBankPackage = lowerPkg.contains("vietcombank") ||
                lowerPkg.contains("tcb") ||
                lowerPkg.contains("mbmobile") ||
                lowerPkg.contains("bidv") ||
                lowerPkg.contains("acb") ||
                lowerPkg.contains("vpb") ||
                lowerPkg.contains("cake") ||
                lowerPkg.contains("sacombank") ||
                lowerPkg.contains("tpbank") ||
                lowerPkg.contains("msb") ||
                lowerPkg.contains("agribank")

        // 2. Kiểm tra từ khóa giao dịch tài chính thông dụng
        val hasKeywords = (lowerText.contains("số dư") || lowerText.contains("so du") ||
                lowerText.contains("biến động") || lowerText.contains("bien dong") ||
                lowerText.contains("tài khoản") || lowerText.contains("tai khoan") ||
                lowerText.contains("giao dịch") || lowerText.contains("giao dich") ||
                lowerText.contains("vnd") || lowerText.contains("chuyển khoản") ||
                lowerText.contains("chuyen khoan")) &&
                (lowerText.contains("+") || lowerText.contains("-") || 
                 lowerText.contains("cộng") || lowerText.contains("cong") ||
                 lowerText.contains("trừ") || lowerText.contains("tru") ||
                 lowerText.contains("tăng") || lowerText.contains("tang") ||
                 lowerText.contains("giảm") || lowerText.contains("giam"))

        return isBankPackage || hasKeywords
    }

    /**
     * Quy trình xử lý và đồng bộ thông báo thô vào Transaction và BankAccount
     */
    private suspend fun processNotification(packageName: String, title: String, text: String, timestamp: Long) {
        try {
            // Bước 1: Xác định tên ngân hàng dựa trên Package hoặc Tiêu đề thông báo
            val bankName = detectBankName(packageName, title)

            // Bước 2: Lưu thông báo thô dạng chưa xử lý (isProcessed = false)
            val rawNotification = RawNotification(
                bankName = bankName,
                fullContent = text,
                receivedAt = timestamp,
                isProcessed = false
            )
            val insertedNotificationId = rawNotificationRepository.insertNotification(rawNotification)
            Log.d("BankNotificationService", "Đã lưu thông báo thô vào Database, ID: $insertedNotificationId")

            // Bước 3: Gọi bộ lọc Regex để phân tích cú pháp
            val parsedData = NotificationParser.parse(bankName, text, timestamp)

            if (parsedData != null) {
                Log.d("BankNotificationService", "Phân tích thành công: Bank=$bankName, Acc=${parsedData.accountNumber}, Amount=${parsedData.amount}")

                // Bước 4: Kiểm tra và lấy/tạo BankAccount tương ứng
                var bankAccount = bankAccountRepository.getBankAccountByNumber(parsedData.accountNumber)
                val bankAccountId: Long

                if (bankAccount == null) {
                    // Nếu là tài khoản ngân hàng mới xuất hiện lần đầu, tự động tạo mới
                    val colorHex = getBankColor(bankName)
                    val newBankAccount = BankAccount(
                        bankName = bankName,
                        accountNumber = parsedData.accountNumber,
                        iconRes = "account_balance", // Icon mặc định
                        colorHex = colorHex
                    )
                    bankAccountId = bankAccountRepository.insertBankAccount(newBankAccount)
                    Log.d("BankNotificationService", "Tự động tạo tài khoản ngân hàng mới, ID: $bankAccountId")
                } else {
                    bankAccountId = bankAccount.id
                    Log.d("BankNotificationService", "Sử dụng tài khoản ngân hàng đã có, ID: $bankAccountId")
                }

                // Bước 5: Ghi nhận Giao dịch chi tiêu/thu nhập
                // Chi tiêu (expense) ghi số âm, thu nhập (income) ghi số dương
                val txAmount = if (parsedData.isCredit) parsedData.amount else -parsedData.amount
                val transaction = Transaction(
                    rawNotificationId = insertedNotificationId,
                    bankAccountId = bankAccountId,
                    amount = txAmount,
                    counterparty = parsedData.counterparty,
                    content = parsedData.content,
                    timestamp = parsedData.timestamp
                )
                transactionRepository.insertTransaction(transaction)
                Log.d("BankNotificationService", "Đã lưu giao dịch tự động vào Database thành công!")

                // Bước 6: Cập nhật thông báo thô thành đã xử lý
                val processedNotification = rawNotification.copy(
                    id = insertedNotificationId,
                    isProcessed = true
                )
                rawNotificationRepository.updateNotification(processedNotification)
            } else {
                Log.w("BankNotificationService", "Không thể phân tách nội dung thông báo thô bằng các bộ lọc Regex.")
            }
        } catch (e: Exception) {
            Log.e("BankNotificationService", "Lỗi xảy ra trong quá trình xử lý thông báo ngầm: ${e.message}", e)
        }
    }

    /**
     * Nhận dạng tên ngân hàng từ Package Name hoặc Tiêu đề thông báo.
     */
    private fun detectBankName(packageName: String, title: String): String {
        val lowerPkg = packageName.lowercase()
        val lowerTitle = title.lowercase()

        return when {
            lowerPkg.contains("vietcombank") || lowerTitle.contains("vietcombank") || lowerTitle.contains("vcb") -> "Vietcombank"
            lowerPkg.contains("tcb") || lowerTitle.contains("techcombank") || lowerTitle.contains("tcb") -> "Techcombank"
            lowerPkg.contains("mbmobile") || lowerTitle.contains("mb bank") || lowerTitle.contains("mbbank") -> "MB Bank"
            lowerPkg.contains("tpbank") || lowerTitle.contains("tpbank") || lowerTitle.contains("tpb") -> "TPBank"
            lowerPkg.contains("bidv") || lowerTitle.contains("bidv") -> "BIDV"
            lowerPkg.contains("acb") || lowerTitle.contains("acb") -> "ACB"
            lowerPkg.contains("cake") || lowerTitle.contains("cake") -> "Cake by VPBank"
            lowerPkg.contains("vpb") || lowerTitle.contains("vpbank") || lowerTitle.contains("vp bank") -> "VPBank"
            lowerPkg.contains("sacombank") || lowerTitle.contains("sacombank") -> "Sacombank"
            lowerPkg.contains("msb") || lowerTitle.contains("msb") -> "MSB"
            else -> title.takeIf { it.isNotBlank() } ?: "Ngân hàng khác"
        }
    }

    /**
     * Lấy màu sắc đặc trưng của từng ngân hàng để đồng bộ trực quan trên giao diện Dashboard.
     */
    private fun getBankColor(bankName: String): String {
        return when (bankName) {
            "Vietcombank" -> "#4CAF50" // Xanh lá VCB
            "Techcombank" -> "#E53935" // Đỏ TCB
            "MB Bank" -> "#1E88E5"     // Xanh dương MB
            "TPBank" -> "#8E24AA"      // Tím TPBank
            "BIDV" -> "#0D47A1"        // Xanh dương đậm BIDV
            "VPBank" -> "#2E7D32"      // Xanh VPBank
            "Cake by VPBank" -> "#EC407A" // Hồng rực rỡ đặc trưng của Cake
            "Sacombank" -> "#0288D1"   // Xanh Sacombank
            "ACB" -> "#0091EA"         // Xanh ACB
            else -> "#757575"          // Màu xám mặc định
        }
    }
}
