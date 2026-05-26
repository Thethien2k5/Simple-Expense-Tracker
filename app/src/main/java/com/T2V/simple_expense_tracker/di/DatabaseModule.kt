package com.T2V.simple_expense_tracker.di

import android.content.Context
import androidx.room.Room
import com.T2V.simple_expense_tracker.data.local.AppDatabase
import com.T2V.simple_expense_tracker.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    @Singleton
    fun provideBankAccountDao(db: AppDatabase): BankAccountDao {
        return db.bankAccountDao()
    }

    @Provides
    @Singleton
    fun provideRawNotificationDao(db: AppDatabase): RawNotificationDao {
        return db.rawNotificationDao()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(db: AppDatabase): TransactionDao {
        return db.transactionDao()
    }
}
