package com.T2V.simple_expense_tracker.domain.usecase

import com.T2V.simple_expense_tracker.domain.model.BankAccount
import com.T2V.simple_expense_tracker.domain.repository.BankAccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBankAccountsUseCase @Inject constructor(
    private val repository: BankAccountRepository
) {
    operator fun invoke(): Flow<List<BankAccount>> {
        return repository.getAllBankAccounts()
    }
}
