package com.T2V.simple_expense_tracker.domain.model

/**
 * Đại diện cho một tài khoản ngân hàng trong ứng dụng ở tầng nghiệp vụ.
 */
data class BankAccount(
    val id: Long = 0,
    val bankName: String,
    val accountNumber: String,
    val iconRes: String,
    val colorHex: String
)
