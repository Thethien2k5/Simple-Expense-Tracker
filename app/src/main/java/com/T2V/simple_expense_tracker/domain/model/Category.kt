package com.T2V.simple_expense_tracker.domain.model

/**
 * Đại diện cho một danh mục chi tiêu ở tầng nghiệp vụ.
 */
data class Category(
    val id: Long = 0,
    val name: String,
    val iconRes: String,
    val colorHex: String
)
