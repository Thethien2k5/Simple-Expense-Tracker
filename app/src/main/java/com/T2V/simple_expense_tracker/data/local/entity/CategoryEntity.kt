package com.T2V.simple_expense_tracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Đại diện cho các danh mục chi tiêu (ví dụ: Ăn uống, Di chuyển).
 * name được đánh chỉ mục unique để tránh trùng lặp danh mục.
 */
@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconRes: String,
    val colorHex: String
)