package com.T2V.simple_expense_tracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Quy tắc tự động để phân loại giao dịch dựa trên tên đối tác hoặc số tiền.
 */
@Entity(
    tableName = "auto_rules",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class AutoRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val targetName: String,
    val fixedAmount: Double?,
    val categoryId: Long,
    val hitCount: Int = 0
)