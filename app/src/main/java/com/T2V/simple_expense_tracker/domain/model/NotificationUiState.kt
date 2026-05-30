package com.T2V.simple_expense_tracker.domain.model

/**
 * Trạng thái hiển thị của Dialog cảnh báo bất thường số dư.
 */
data class AnomalyUiState(
    val show: Boolean = false,
    val bankName: String = "",
    val currentBalance: Double = 0.0,
    val transactionAmount: Double = 0.0,
    val expectedBalance: Double = 0.0,
    val reportedBalance: Double = 0.0,
    val notificationId: Long = 0,
    val bankAccountId: Long = 0
)

/**
 * Trạng thái hiển thị của Màn hình nhập giao dịch thủ công.
 */
data class ManualParseUiState(
    val show: Boolean = false,
    val bankName: String = "",
    val rawContent: String = "",
    val notificationId: Long = 0,
    val amountText: String = "",
    val isCredit: Boolean = false,
    val accountNumber: String = "",
    val content: String = "",
    val counterparty: String = ""
)
