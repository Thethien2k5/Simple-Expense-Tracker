package com.T2V.simple_expense_tracker.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.T2V.simple_expense_tracker.R
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
 * - Gửi thông báo hệ thống khi phát hiện bất thường hoặc cần xử lý thủ công.
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

    companion object {
        private const val TAG = "BankNotificationService"
        private const val CHANNEL_ID_ANOMALY = "anomaly_channel"
        private const val CHANNEL_ID_MANUAL = "manual_parse_channel"
        private const val NOTIFICATION_ID_ANOMALY_BASE = 10000
        private const val NOTIFICATION_ID_MANUAL_BASE = 20000
        // Ngưỡng sai lệch số dư cho phép (VND) - do làm tròn
        private const val BALANCE_TOLERANCE = 1.0
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
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
            val title = extras.getString("android.title", "")
            val text = extras.getCharSequence("android.text", "").toString()
            val postTime = it.postTime

            Log.d(TAG, "Nhận thông báo từ: $packageName | Tiêu đề: $title | Nội dung: $text")

            // Sử dụng bộ lọc mới: Blacklist SMS, Whitelist ngân hàng, loại bỏ quảng cáo
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
     * 5. Lưu giao dịch hoặc gửi thông báo yêu cầu xác nhận.
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
                    var bankAccount = findBankAccount(parsedData.accountNumber, bankName)
                    val bankAccountId: Long

                    if (bankAccount == null) {
                        // Tài khoản mới: Tạo mới và lưu giao dịch ngay (không kiểm tra số dư)
                        val colorHex = notificationParser.getBankColor(bankName)
                        val initialBalance = parsedData.balance ?: txAmount
                        val newBankAccount = BankAccount(
                            bankName = bankName,
                            accountNumber = parsedData.accountNumber,
                            iconRes = "account_balance",
                            colorHex = colorHex,
                            balance = initialBalance
                        )
                        bankAccountId = bankAccountRepository.insertBankAccount(newBankAccount)
                        Log.d(TAG, "Tự động tạo tài khoản ngân hàng mới, ID: $bankAccountId, Số dư khởi tạo: $initialBalance")

                        // Lưu giao dịch
                        saveTransaction(insertedNotificationId, bankAccountId, txAmount, parsedData, rawNotification)
                    } else {
                        bankAccountId = bankAccount.id

                        // Bước 5: Kiểm tra tính nhất quán số dư (chỉ khi thông báo có số dư)
                        if (parsedData.balance != null) {
                            val expectedBalance = bankAccount.balance + txAmount
                            val reportedBalance = parsedData.balance
                            val difference = abs(reportedBalance - expectedBalance)

                            if (difference > BALANCE_TOLERANCE) {
                                // Phát hiện sai lệch số dư -> Gửi thông báo yêu cầu xác nhận
                                Log.w(TAG, "⚠️ Sai lệch số dư: Dự kiến=$expectedBalance, Thông báo=$reportedBalance, Chênh lệch=$difference")
                                sendAnomalyNotification(
                                    bankName = bankName,
                                    currentBalance = bankAccount.balance,
                                    transactionAmount = txAmount,
                                    expectedBalance = expectedBalance,
                                    reportedBalance = reportedBalance,
                                    notificationId = insertedNotificationId,
                                    bankAccountId = bankAccountId,
                                    parsedData = parsedData
                                )
                                // KHÔNG lưu giao dịch, chờ người dùng xác nhận
                                return
                            }
                        }

                        // Số dư nhất quán hoặc thông báo không có số dư -> Lưu bình thường
                        val finalBalance = parsedData.balance ?: (bankAccount.balance + txAmount)
                        val updatedBankAccount = bankAccount.copy(balance = finalBalance)
                        bankAccountRepository.updateBankAccount(updatedBankAccount)
                        Log.d(TAG, "Cập nhật tài khoản, ID: $bankAccountId, Số dư mới: $finalBalance")

                        // Lưu giao dịch
                        saveTransaction(insertedNotificationId, bankAccountId, txAmount, parsedData, rawNotification)
                    }
                }

                ParseResult.NEEDS_MANUAL -> {
                    // Regex và ML Kit đều thất bại -> Gửi thông báo yêu cầu xử lý thủ công
                    Log.w(TAG, "⚠️ Không thể phân tích thông báo từ $bankName. Yêu cầu xử lý thủ công.")
                    sendManualParseNotification(bankName, text, insertedNotificationId)
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

    // ============================================================================
    // THÔNG BÁO HỆ THỐNG (System Notifications)
    // ============================================================================

    /**
     * Tạo các kênh thông báo (bắt buộc từ Android 8.0+).
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Kênh thông báo bất thường số dư
            val anomalyChannel = NotificationChannel(
                CHANNEL_ID_ANOMALY,
                "Cảnh báo bất thường số dư",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo khi phát hiện sai lệch số dư ngân hàng."
            }
            notificationManager.createNotificationChannel(anomalyChannel)

            // Kênh thông báo xử lý thủ công
            val manualChannel = NotificationChannel(
                CHANNEL_ID_MANUAL,
                "Yêu cầu xử lý thủ công",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Thông báo khi cần người dùng xử lý thông báo ngân hàng thủ công."
            }
            notificationManager.createNotificationChannel(manualChannel)
        }
    }

    /**
     * Gửi thông báo khi phát hiện sai lệch số dư.
     */
    private fun sendAnomalyNotification(
        bankName: String,
        currentBalance: Double,
        transactionAmount: Double,
        expectedBalance: Double,
        reportedBalance: Double,
        notificationId: Long,
        bankAccountId: Long,
        parsedData: ParsedData
    ) {
        val formatter = java.text.DecimalFormat("#,###")
        val difference = reportedBalance - expectedBalance

        // Intent mở ứng dụng (MainActivity) khi nhấn vào thông báo
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", "anomaly_confirmation")
            putExtra("bankName", bankName)
            putExtra("currentBalance", currentBalance)
            putExtra("transactionAmount", transactionAmount)
            putExtra("expectedBalance", expectedBalance)
            putExtra("reportedBalance", reportedBalance)
            putExtra("notificationId", notificationId)
            putExtra("bankAccountId", bankAccountId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, notificationId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_ANOMALY)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Bất thường số dư - $bankName")
            .setContentText("Chênh lệch ${formatter.format(difference)} VND. Nhấn để xem chi tiết.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Số dư hiện tại: ${formatter.format(currentBalance)} VND\n" +
                        "Giao dịch: ${formatter.format(transactionAmount)} VND\n" +
                        "Số dư dự kiến: ${formatter.format(expectedBalance)} VND\n" +
                        "Số dư thông báo: ${formatter.format(reportedBalance)} VND\n" +
                        "Chênh lệch: ${formatter.format(difference)} VND"
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID_ANOMALY_BASE + notificationId.toInt(), notification)
    }

    /**
     * Gửi thông báo yêu cầu xử lý thủ công khi cả Regex và ML Kit đều thất bại.
     */
    private fun sendManualParseNotification(bankName: String, rawContent: String, notificationId: Long) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", "manual_parse")
            putExtra("bankName", bankName)
            putExtra("rawContent", rawContent)
            putExtra("notificationId", notificationId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, (notificationId + 50000).toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_MANUAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📋 Cần xử lý thủ công - $bankName")
            .setContentText("Không thể phân tích thông báo tự động. Nhấn để nhập thủ công.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Nội dung thông báo:\n\"$rawContent\"\n\nNhấn để mở giao diện nhập thủ công."
            ))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID_MANUAL_BASE + notificationId.toInt(), notification)
    }
}
