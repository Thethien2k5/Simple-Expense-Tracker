package com.T2V.simple_expense_tracker.ui.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.T2V.simple_expense_tracker.domain.model.RawNotification
import com.T2V.simple_expense_tracker.domain.usecase.GetAllNotificationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel cho panel "Danh sách thông báo" (drawer phải).
 * Hiển thị toàn bộ thông báo thô từ hệ thống — bao gồm cả đã và chưa xử lý.
 */
data class NotificationUiState(
    val isLoading: Boolean = true,
    val notifications: List<RawNotification> = emptyList()
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    getAllNotificationsUseCase: GetAllNotificationsUseCase
) : ViewModel() {
    val uiState: StateFlow<NotificationUiState> = getAllNotificationsUseCase()
        .map { notifications ->
            NotificationUiState(isLoading = false, notifications = notifications)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotificationUiState()
        )
}
