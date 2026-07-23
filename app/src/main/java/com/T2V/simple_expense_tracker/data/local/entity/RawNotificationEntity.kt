package com.T2V.simple_expense_tracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 *  Định nghĩa cấu trúc của bảng *thông báo thô*
 */
@Entity(
    tableName = "raw_notifications",
    indices = [Index(value = ["isProcessed"])] // khai báo chỉ mục hộ trợ tìm kiếm nhanh
)
data class RawNotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bankName: String,
    val fullContent: String,
    val receivedAt: Long,
    val isProcessed: Boolean = false
)