package com.T2V.simple_expense_tracker.data.local.dao

import androidx.room.*
import com.T2V.simple_expense_tracker.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
  * Thực hiện truy vấn với bảng *giao dịch*
 */
@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) // Ghi đè bản ghi cũ trong TH trùng
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC") // lấy toàn bộ và sắp mới->cữ theo tg g/dịch
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    // Lấy giao dịch theo tài khoản và sắp xếp
    @Query("SELECT * FROM transactions WHERE bankAccountId = :bankAccountId ORDER BY timestamp DESC")
    fun getTransactionsByBank(bankAccountId: Long): Flow<List<TransactionEntity>>
}