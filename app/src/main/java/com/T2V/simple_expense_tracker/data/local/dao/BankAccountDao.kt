package com.T2V.simple_expense_tracker.data.local.dao

import androidx.room.*
import com.T2V.simple_expense_tracker.data.local.entity.BankAccountEntity
import kotlinx.coroutines.flow.Flow

/**
 * Giao diện truy cập dữ liệu cho tài khoản ngân hàng.
 */
@Dao
interface BankAccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankAccount(bankAccount: BankAccountEntity): Long

    @Update
    suspend fun updateBankAccount(bankAccount: BankAccountEntity)

    @Delete
    suspend fun deleteBankAccount(bankAccount: BankAccountEntity)

    @Query("SELECT * FROM bank_accounts")
    fun getAllBankAccounts(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts WHERE id = :id")
    suspend fun getBankAccountById(id: Long): BankAccountEntity?

    @Query("SELECT * FROM bank_accounts WHERE accountNumber = :accountNumber LIMIT 1")
    suspend fun getBankAccountByNumber(accountNumber: String): BankAccountEntity?
}