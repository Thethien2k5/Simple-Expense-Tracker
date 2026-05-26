package com.T2V.simple_expense_tracker.di

import com.T2V.simple_expense_tracker.data.repository.*
import com.T2V.simple_expense_tracker.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBankAccountRepository(
        bankAccountRepositoryImpl: BankAccountRepositoryImpl
    ): BankAccountRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepositoryImpl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindRawNotificationRepository(
        rawNotificationRepositoryImpl: RawNotificationRepositoryImpl
    ): RawNotificationRepository

}
