package com.T2V.simple_expense_tracker.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.T2V.simple_expense_tracker.domain.repository.ThemeRepository
import com.T2V.simple_expense_tracker.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ThemeRepository {
    private val THEME_KEY = stringPreferencesKey("selected_theme")

    override val selectedTheme: Flow<AppTheme> = dataStore.data.map { preferences ->
        val themeName = preferences[THEME_KEY] ?: AppTheme.EMERALD.name
        try {
            // Chuyển cái tên (chuỗi chữ) thành kiểu AppTheme
            AppTheme.valueOf(themeName)
        } catch (e: Exception) {
            AppTheme.EMERALD
        }
    }

    override suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }
}