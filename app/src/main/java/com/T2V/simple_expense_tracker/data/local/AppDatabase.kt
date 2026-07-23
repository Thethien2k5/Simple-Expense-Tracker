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
 * Khởi tạo và cấu hình cơ sở dữ liệu
 */
@Database(
    entities = [
        BankAccountEntity::class,
        RawNotificationEntity::class,
        TransactionEntity::class
    ],
    version = 4,
    exportSchema = false // không xuất json khi bulid
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bankAccountDao(): BankAccountDao // kết nối để thực thi lệnh trong DAO
    abstract fun rawNotificationDao(): RawNotificationDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        const val DATABASE_NAME = "expense_tracker_db"
    }
}