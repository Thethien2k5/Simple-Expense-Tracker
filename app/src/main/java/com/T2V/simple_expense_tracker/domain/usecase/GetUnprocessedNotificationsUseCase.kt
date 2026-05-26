package com.T2V.simple_expense_tracker.domain.usecase

import com.T2V.simple_expense_tracker.domain.model.RawNotification
import com.T2V.simple_expense_tracker.domain.repository.RawNotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUnprocessedNotificationsUseCase @Inject constructor(
    private val repository: RawNotificationRepository
) {
    operator fun invoke(): Flow<List<RawNotification>> {
        return repository.getUnprocessedNotifications()
    }
}
