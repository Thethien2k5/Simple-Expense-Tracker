package com.T2V.simple_expense_tracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.T2V.simple_expense_tracker.data.local.dao.*
import com.T2V.simple_expense_tracker.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Cơ sở dữ liệu chính của ứng dụng sử dụng Room.
 * Bao gồm 3 bảng: BankAccount, RawNotification và Transaction.
 */
@Database(
    entities = [
        BankAccountEntity::class,
        RawNotificationEntity::class,
        TransactionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bankAccountDao(): BankAccountDao
    abstract fun rawNotificationDao(): RawNotificationDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        const val DATABASE_NAME = "expense_tracker_db"
    }
}