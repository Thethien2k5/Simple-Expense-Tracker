package com.T2V.simple_expense_tracker.domain.usecase

import com.T2V.simple_expense_tracker.domain.model.Transaction
import com.T2V.simple_expense_tracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<List<Transaction>> {
        return repository.getAllTransactions()
    }
}
