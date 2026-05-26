package com.T2V.simple_expense_tracker.domain.repository

import com.T2V.simple_expense_tracker.domain.model.RawNotification
import kotlinx.coroutines.flow.Flow

interface RawNotificationRepository {
    suspend fun insertNotification(notification: RawNotification): Long
    suspend fun updateNotification(notification: RawNotification)
    fun getUnprocessedNotifications(): Flow<List<RawNotification>>
    fun getAllNotifications(): Flow<List<RawNotification>>
}
