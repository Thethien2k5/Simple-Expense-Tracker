package com.T2V.simple_expense_tracker.domain.repository

import com.T2V.simple_expense_tracker.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    // hiển thị danh sách theme
    val selectedTheme: Flow<AppTheme>

    //Chọn theme
    suspend fun setTheme(theme: AppTheme)
}