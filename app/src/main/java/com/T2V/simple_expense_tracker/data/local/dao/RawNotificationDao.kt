package com.T2V.simple_expense_tracker.data.local.dao

import androidx.room.*
import com.T2V.simple_expense_tracker.data.local.entity.RawNotificationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Giao diện truy cập dữ liệu cho thông báo thô.
 */
@Dao
interface RawNotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: RawNotificationEntity): Long

    @Update
    suspend fun updateNotification(notification: RawNotificationEntity)

    @Query("SELECT * FROM raw_notifications WHERE isProcessed = 0")
    fun getUnprocessedNotifications(): Flow<List<RawNotificationEntity>>

    @Query("SELECT * FROM raw_notifications ORDER BY receivedAt DESC")
    fun getAllNotifications(): Flow<List<RawNotificationEntity>>
}