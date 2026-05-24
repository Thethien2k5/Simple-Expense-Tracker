package com.T2V.simple_expense_tracker.data.local.dao

import androidx.room.*
import com.T2V.simple_expense_tracker.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Giao diện truy cập dữ liệu cho các giao dịch tài chính.
 */
@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE bankAccountId = :bankAccountId ORDER BY timestamp DESC")
    fun getTransactionsByBank(bankAccountId: Long): Flow<List<TransactionEntity>>
}