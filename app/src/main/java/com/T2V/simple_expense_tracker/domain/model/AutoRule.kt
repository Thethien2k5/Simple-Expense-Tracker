package com.T2V.simple_expense_tracker.domain.model

/**
 * Quy tắc tự động để phân loại giao dịch ở tầng nghiệp vụ.
 */
data class AutoRule(
    val id: Long = 0,
    val targetName: String,
    val fixedAmount: Double?,
    val categoryId: Long,
    val hitCount: Int = 0
)
