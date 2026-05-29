package com.T2V.simple_expense_tracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.T2V.simple_expense_tracker.domain.repository.AppLanguage
import com.T2V.simple_expense_tracker.domain.repository.LanguageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) : LanguageRepository {
    private val LANGUAGE_KEY = stringPreferencesKey("selected_language")

    override val selectedLanguage: Flow<AppLanguage> = dataStore.data
        .map { preferences ->
            val code = preferences[LANGUAGE_KEY] ?: "vi"
            AppLanguage.entries.find { it.code == code } ?: AppLanguage.VIETNAMESE
        }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.code
        }
    }
}
