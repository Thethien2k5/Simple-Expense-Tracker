package com.T2V.simple_expense_tracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Định nghĩa cấu trúc của bảng *tài khoản ngân hàng*
 */
@Entity(
    tableName = "bank_accounts", // tên bảng trong db
    indices = [Index(value = ["bankName", "accountNumber"], unique = true)] //khai báo chỉ mục "unique = true" không trùng
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