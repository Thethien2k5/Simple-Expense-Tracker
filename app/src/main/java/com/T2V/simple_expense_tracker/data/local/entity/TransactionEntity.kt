package com.T2V.simple_expense_tracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 *  Định nghĩa cấu trúc của bảng *giao dịch*
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = RawNotificationEntity::class,
            parentColumns = ["id"],
            childColumns = ["rawNotificationId"],
            onDelete = ForeignKey.CASCADE // xóa dây truyền
        ),
        ForeignKey(
            entity = BankAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["bankAccountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("rawNotificationId"),
        Index("bankAccountId")
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
    val timestamp: Long
)