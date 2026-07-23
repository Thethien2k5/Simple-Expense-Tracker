package com.T2V.simple_expense_tracker.data.local.dao

import androidx.room.*
import com.T2V.simple_expense_tracker.data.local.entity.RawNotificationEntity
import kotlinx.coroutines.flow.Flow

/**
 * Thực hiện truy vấn với bảng *thông báo thô*
 */
@Dao
interface RawNotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) // Ghi đè bản ghi cũ trong TH trùng
    suspend fun insertNotification(notification: RawNotificationEntity): Long

    @Update
    suspend fun updateNotification(notification: RawNotificationEntity)

    @Query("SELECT * FROM raw_notifications WHERE isProcessed = 0")
    fun getUnprocessedNotifications(): Flow<List<RawNotificationEntity>>

    @Query("SELECT * FROM raw_notifications WHERE id = :id")
    suspend fun getNotificationById(id: Long): RawNotificationEntity?

    //"ORDER BY receivedAt DESC" sắp xếp kq t/vể từ mới->cũ theo "receivedAt" *thời gian tạo*
    @Query("SELECT * FROM raw_notifications ORDER BY receivedAt DESC")
    fun getAllNotifications(): Flow<List<RawNotificationEntity>>
}