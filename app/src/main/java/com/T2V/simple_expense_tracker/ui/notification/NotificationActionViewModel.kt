package com.T2V.simple_expense_tracker.ui.notification

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.T2V.simple_expense_tracker.domain.model.*
import com.T2V.simple_expense_tracker.domain.parser.NotificationParser
import com.T2V.simple_expense_tracker.domain.repository.BankAccountRepository
import com.T2V.simple_expense_tracker.domain.repository.RawNotificationRepository
import com.T2V.simple_expense_tracker.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationActionViewModel @Inject constructor(
    private val rawNotificationRepository: RawNotificationRepository,
    private val bankAccountRepository: BankAccountRepository,
    private val transactionRepository: TransactionRepository,
    private val notificationParser: NotificationParser
) : ViewModel() {

    private val _anomalyUiState = MutableStateFlow(AnomalyUiState())
    val anomalyUiState: StateFlow<AnomalyUiState> = _anomalyUiState.asStateFlow()

    private val _manualParseUiState = MutableStateFlow(ManualParseUiState())
    val manualParseUiState: StateFlow<ManualParseUiState> = _manualParseUiState.asStateFlow()

    fun handleIntent(intent: Intent) {
        val action = intent.getStringExtra("action") ?: intent.action
        Log.d("NotificationActionVM", "Handling intent action: $action")
        if (action == "anomaly_confirmation") {
            val bankName = intent.getStringExtra("bankName") ?: ""
            val currentBalance = intent.getDoubleExtra("currentBalance", 0.0)
            val transactionAmount = intent.getDoubleExtra("transactionAmount", 0.0)
            val expectedBalance = intent.getDoubleExtra("expectedBalance", 0.0)
            val reportedBalance = intent.getDoubleExtra("reportedBalance", 0.0)
            val notificationId = intent.getLongExtra("notificationId", 0L)
            val bankAccountId = intent.getLongExtra("bankAccountId", 0L)

            _anomalyUiState.value = AnomalyUiState(
                show = true,
                bankName = bankName,
                currentBalance = currentBalance,
                transactionAmount = transactionAmount,
                expectedBalance = expectedBalance,
                reportedBalance = reportedBalance,
                notificationId = notificationId,
                bankAccountId = bankAccountId
            )
        } else if (action == "manual_parse") {
            val bankName = intent.getStringExtra("bankName") ?: ""
            val rawContent = intent.getStringExtra("rawContent") ?: ""
            val notificationId = intent.getLongExtra("notificationId", 0L)

            _manualParseUiState.value = ManualParseUiState(
                show = true,
                bankName = bankName,
                rawContent = rawContent,
                notificationId = notificationId
            )
        }
    }

    fun confirmAnomaly() {
        val state = _anomalyUiState.value
        if (!state.show) return

        viewModelScope.launch {
            try {
                // 1. Cập nhật trạng thái thông báo thô thành đã xử lý
                val rawNotification = rawNotificationRepository.getNotificationById(state.notificationId)
                if (rawNotification != null) {
                    rawNotificationRepository.updateNotification(rawNotification.copy(isProcessed = true))
                }

                // 2. Cập nhật số dư tài khoản ngân hàng thành số dư thực tế từ thông báo
                val bankAccount = bankAccountRepository.getBankAccountById(state.bankAccountId)
                if (bankAccount != null) {
                    bankAccountRepository.updateBankAccount(bankAccount.copy(balance = state.reportedBalance))
                }

                // 3. Phân tích nội dung thô để lấy chi tiết giao dịch (nội dung, đối tác)
                var counterparty = "Chênh lệch số dư"
                var content = "Điều chỉnh số dư do phát hiện sai lệch"
                if (rawNotification != null) {
                    val parsed = notificationParser.parse(state.bankName, rawNotification.fullContent, rawNotification.receivedAt)
                    if (parsed != null) {
                        counterparty = parsed.counterparty
                        content = parsed.content
                    }
                }

                // 4. Lưu giao dịch
                val transaction = Transaction(
                    rawNotificationId = state.notificationId,
                    bankAccountId = state.bankAccountId,
                    amount = state.transactionAmount,
                    counterparty = counterparty,
                    content = content,
                    timestamp = rawNotification?.receivedAt ?: System.currentTimeMillis()
                )
                transactionRepository.insertTransaction(transaction)
                Log.d("NotificationActionVM", "Đã xác nhận và điều chỉnh số dư thành công!")
            } catch (e: Exception) {
                Log.e("NotificationActionVM", "Lỗi confirmAnomaly: ${e.message}", e)
            } finally {
                dismissAnomalyDialog()
            }
        }
    }

    fun rejectAnomaly() {
        val state = _anomalyUiState.value
        if (!state.show) return

        viewModelScope.launch {
            try {
                // Chỉ đánh dấu thông báo thô là đã xử lý (bỏ qua không lưu giao dịch)
                val rawNotification = rawNotificationRepository.getNotificationById(state.notificationId)
                if (rawNotification != null) {
                    rawNotificationRepository.updateNotification(rawNotification.copy(isProcessed = true))
                }
                Log.d("NotificationActionVM", "Đã từ chối giao dịch bất thường số dư.")
            } catch (e: Exception) {
                Log.e("NotificationActionVM", "Lỗi rejectAnomaly: ${e.message}", e)
            } finally {
                dismissAnomalyDialog()
            }
        }
    }

    fun saveManualParse(
        amount: Double,
        isCredit: Boolean,
        accountNumber: String,
        content: String,
        counterparty: String
    ) {
        val state = _manualParseUiState.value
        if (!state.show) return

        viewModelScope.launch {
            try {
                val rawNotification = rawNotificationRepository.getNotificationById(state.notificationId)
                val timestamp = rawNotification?.receivedAt ?: System.currentTimeMillis()
                val txAmount = if (isCredit) amount else -amount

                // 1. Tìm hoặc tạo tài khoản ngân hàng
                var bankAccount = findBankAccount(accountNumber, state.bankName)
                val bankAccountId: Long
                if (bankAccount == null) {
                    val colorHex = notificationParser.getBankColor(state.bankName)
                    val newAccount = BankAccount(
                        bankName = state.bankName,
                        accountNumber = if (accountNumber.isNotEmpty()) accountNumber else "DEFAULT_ACC",
                        iconRes = "account_balance",
                        colorHex = colorHex,
                        balance = txAmount
                    )
                    bankAccountId = bankAccountRepository.insertBankAccount(newAccount)
                } else {
                    bankAccountId = bankAccount.id
                    val updatedAccount = bankAccount.copy(balance = bankAccount.balance + txAmount)
                    bankAccountRepository.updateBankAccount(updatedAccount)
                }

                // 2. Lưu giao dịch
                val transaction = Transaction(
                    rawNotificationId = state.notificationId,
                    bankAccountId = bankAccountId,
                    amount = txAmount,
                    counterparty = counterparty.ifEmpty { "Nhập thủ công" },
                    content = content.ifEmpty { "Giao dịch thủ công" },
                    timestamp = timestamp
                )
                transactionRepository.insertTransaction(transaction)

                // 3. Cập nhật thông báo thô thành đã xử lý
                if (rawNotification != null) {
                    rawNotificationRepository.updateNotification(rawNotification.copy(isProcessed = true))
                }

                Log.d("NotificationActionVM", "Đã lưu giao dịch thủ công thành công!")
            } catch (e: Exception) {
                Log.e("NotificationActionVM", "Lỗi saveManualParse: ${e.message}", e)
            } finally {
                dismissManualParseScreen()
            }
        }
    }

    fun dismissAnomalyDialog() {
        _anomalyUiState.value = AnomalyUiState(show = false)
    }

    fun dismissManualParseScreen() {
        val state = _manualParseUiState.value
        if (state.show) {
            viewModelScope.launch {
                // Đánh dấu thông báo thô là đã xử lý để tránh hiển thị lại
                val rawNotification = rawNotificationRepository.getNotificationById(state.notificationId)
                if (rawNotification != null) {
                    rawNotificationRepository.updateNotification(rawNotification.copy(isProcessed = true))
                }
                _manualParseUiState.value = ManualParseUiState(show = false)
            }
        }
    }

    private suspend fun findBankAccount(accountNumber: String, bankName: String): BankAccount? {
        if (accountNumber.isNotEmpty() && accountNumber != "DEFAULT_ACC") {
            val account = bankAccountRepository.getBankAccountByNumber(accountNumber)
            if (account != null) return account
        }
        return bankAccountRepository.getBankAccountByName(bankName)
    }
}
