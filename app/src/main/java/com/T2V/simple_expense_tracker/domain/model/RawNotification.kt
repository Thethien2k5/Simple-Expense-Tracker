package com.T2V.simple_expense_tracker.domain.model

/**
 * Đại diện cho thông báo thô từ ngân hàng ở tầng nghiệp vụ.
 */
data class RawNotification(
    val id: Long = 0,
    val bankName: String,
    val fullContent: String,
    val receivedAt: Long,
    val isProcessed: Boolean = false
)
