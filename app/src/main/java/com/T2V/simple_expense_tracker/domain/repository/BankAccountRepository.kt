package com.T2V.simple_expense_tracker.domain.repository

import com.T2V.simple_expense_tracker.domain.model.BankAccount
import kotlinx.coroutines.flow.Flow

interface BankAccountRepository {
    suspend fun insertBankAccount(bankAccount: BankAccount): Long
    suspend fun updateBankAccount(bankAccount: BankAccount)
    suspend fun deleteBankAccount(bankAccount: BankAccount)
    fun getAllBankAccounts(): Flow<List<BankAccount>>
    suspend fun getBankAccountById(id: Long): BankAccount?
    suspend fun getBankAccountByNumber(accountNumber: String): BankAccount?
}
