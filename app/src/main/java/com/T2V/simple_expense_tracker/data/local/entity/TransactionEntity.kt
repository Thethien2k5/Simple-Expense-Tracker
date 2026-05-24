package com.T2V.simple_expense_tracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Lưu trữ thông tin giao dịch đã được phân tích.
 * Có các khóa ngoại liên kết tới thông báo thô, tài khoản ngân hàng và danh mục.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = RawNotificationEntity::class,
            parentColumns = ["id"],
            childColumns = ["rawNotificationId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BankAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["bankAccountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_DEFAULT
        )
    ],
    indices = [
        Index("rawNotificationId"),
        Index("bankAccountId"),
        Index("categoryId")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawNotificationId: Long,
    val bankAccountId: Long,
    val amount: Double,
    val counterparty: String,
    val content: String,
    val categoryId: Long,
    val timestamp: Long
)