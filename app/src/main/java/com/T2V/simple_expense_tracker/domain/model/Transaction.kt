package com.T2V.simple_expense_tracker.domain.model

/**
 * Đại diện cho một giao dịch tài chính đã được phân tích ở tầng nghiệp vụ.
 */
data class Transaction(
    val id: Long = 0,
    val rawNotificationId: Long,
    val bankAccountId: Long,
    val amount: Double,
    val counterparty: String,
    val content: String,
    val timestamp: Long
)
