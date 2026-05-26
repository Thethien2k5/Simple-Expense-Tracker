package com.T2V.simple_expense_tracker.ui.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Trạng thái UI của Panel Cài đặt.
 * Sau khi lược bỏ hoàn toàn Category, hiện tại chỉ giữ khung trạng thái đơn giản.
 */
data class SettingsUiState(
    val isLoading: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
}
