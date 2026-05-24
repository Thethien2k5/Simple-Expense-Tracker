package com.T2V.simple_expense_tracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Lưu trữ nội dung thông báo thô từ ngân hàng.
 * isProcessed được đánh chỉ mục (Index) để tối ưu việc lọc các thông báo chưa xử lý.
 */
@Entity(
    tableName = "raw_notifications",
    indices = [Index(value = ["isProcessed"])]
)
data class RawNotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bankName: String,
    val fullContent: String,
    val receivedAt: Long,
    val isProcessed: Boolean = false
)