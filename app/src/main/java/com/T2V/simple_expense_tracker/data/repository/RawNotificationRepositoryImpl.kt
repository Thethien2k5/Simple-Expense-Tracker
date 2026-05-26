package com.T2V.simple_expense_tracker.data.repository

import com.T2V.simple_expense_tracker.data.local.dao.RawNotificationDao
import com.T2V.simple_expense_tracker.data.mapper.toDomain
import com.T2V.simple_expense_tracker.data.mapper.toEntity
import com.T2V.simple_expense_tracker.domain.model.RawNotification
import com.T2V.simple_expense_tracker.domain.repository.RawNotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RawNotificationRepositoryImpl @Inject constructor(
    private val rawNotificationDao: RawNotificationDao
) : RawNotificationRepository {

    override suspend fun insertNotification(notification: RawNotification): Long {
        return rawNotificationDao.insertNotification(notification.toEntity())
    }

    override suspend fun updateNotification(notification: RawNotification) {
        rawNotificationDao.updateNotification(notification.toEntity())
    }

    override fun getUnprocessedNotifications(): Flow<List<RawNotification>> {
        return rawNotificationDao.getUnprocessedNotifications().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllNotifications(): Flow<List<RawNotification>> {
        return rawNotificationDao.getAllNotifications().map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
