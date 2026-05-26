package com.T2V.simple_expense_tracker.domain.repository

import com.T2V.simple_expense_tracker.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun insertTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsByBank(bankAccountId: Long): Flow<List<Transaction>>
}
