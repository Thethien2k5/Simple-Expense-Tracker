package com.T2V.simple_expense_tracker.domain.usecase

import com.T2V.simple_expense_tracker.domain.model.RawNotification
import com.T2V.simple_expense_tracker.domain.repository.RawNotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Lấy toàn bộ thông báo thô (bao gồm đã xử lý và chưa xử lý).
 * Dùng cho màn "Danh sách thông báo" — hiển thị tất cả notification gốc.
 */
class GetAllNotificationsUseCase @Inject constructor(
    private val repository: RawNotificationRepository
) {
    operator fun invoke(): Flow<List<RawNotification>> {
        return repository.getAllNotifications()
    }
}
