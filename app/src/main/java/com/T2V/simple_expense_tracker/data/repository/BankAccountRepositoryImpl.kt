package com.T2V.simple_expense_tracker.data.repository

import com.T2V.simple_expense_tracker.data.local.dao.BankAccountDao
import com.T2V.simple_expense_tracker.data.mapper.toDomain
import com.T2V.simple_expense_tracker.data.mapper.toEntity
import com.T2V.simple_expense_tracker.domain.model.BankAccount
import com.T2V.simple_expense_tracker.domain.repository.BankAccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BankAccountRepositoryImpl @Inject constructor(
    private val bankAccountDao: BankAccountDao
) : BankAccountRepository {

    override suspend fun insertBankAccount(bankAccount: BankAccount): Long {
        return bankAccountDao.insertBankAccount(bankAccount.toEntity())
    }

    override suspend fun updateBankAccount(bankAccount: BankAccount) {
        bankAccountDao.updateBankAccount(bankAccount.toEntity())
    }

    override suspend fun deleteBankAccount(bankAccount: BankAccount) {
        bankAccountDao.deleteBankAccount(bankAccount.toEntity())
    }

    override fun getAllBankAccounts(): Flow<List<BankAccount>> {
        return bankAccountDao.getAllBankAccounts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getBankAccountById(id: Long): BankAccount? {
        return bankAccountDao.getBankAccountById(id)?.toDomain()
    }

    override suspend fun getBankAccountByNumber(accountNumber: String): BankAccount? {
        return bankAccountDao.getBankAccountByNumber(accountNumber)?.toDomain()
    }

    override suspend fun getBankAccountByName(bankName: String): BankAccount? {
        return bankAccountDao.getBankAccountByName(bankName)?.toDomain()
    }
}
