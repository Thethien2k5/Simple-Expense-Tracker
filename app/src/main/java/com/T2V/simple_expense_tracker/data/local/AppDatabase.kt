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
 * Bao gồm 5 bảng: BankAccount, RawNotification, Transaction, Category và AutoRule.
 */
@Database(
    entities = [
        BankAccountEntity::class,
        RawNotificationEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        AutoRuleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bankAccountDao(): BankAccountDao
    abstract fun rawNotificationDao(): RawNotificationDao
    abstract fun categoryDao(): CategoryDao
    abstract fun autoRuleDao(): AutoRuleDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        const val DATABASE_NAME = "expense_tracker_db"

        /**
         * Callback để khởi tạo dữ liệu mặc định khi cơ sở dữ liệu được tạo lần đầu.
         */
        val CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Seed dữ liệu mặc định: Danh mục "Chưa phân loại"
                CoroutineScope(Dispatchers.IO).launch {
                    // Chèn trực tiếp qua SQL để đảm bảo thực thi ngay khi tạo DB
                    db.execSQL(
                        "INSERT INTO categories (id, name, iconRes, colorHex) " +
                        "VALUES (1, 'Chưa phân loại', 'ic_uncategorized', '#757575')"
                    )
                }
            }
        }
    }
}