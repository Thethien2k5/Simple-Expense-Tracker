package com.T2V.simple_expense_tracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Đại diện cho một tài khoản ngân hàng trong ứng dụng.
 * Lưu trữ thông tin cơ bản về ngân hàng để phân loại giao dịch.
 */
@Entity(
    tableName = "bank_accounts",
    indices = [Index(value = ["bankName", "accountNumber"], unique = true)]
)
data class BankAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bankName: String,
    val accountNumber: String,
    val iconRes: String,
    val colorHex: String,
    val balance: Double = 0.0
)