package com.T2V.simple_expense_tracker.data.repository

import com.T2V.simple_expense_tracker.data.local.dao.TransactionDao
import com.T2V.simple_expense_tracker.data.mapper.toDomain
import com.T2V.simple_expense_tracker.data.mapper.toEntity
import com.T2V.simple_expense_tracker.domain.model.Transaction
import com.T2V.simple_expense_tracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction.toEntity())
    }

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTransactionsByBank(bankAccountId: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByBank(bankAccountId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
