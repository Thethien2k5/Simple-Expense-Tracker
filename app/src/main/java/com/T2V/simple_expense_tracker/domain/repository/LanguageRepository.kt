package com.T2V.simple_expense_tracker.domain.repository

import kotlinx.coroutines.flow.Flow

interface LanguageRepository {
    val selectedLanguage: Flow<AppLanguage>
    suspend fun setLanguage(language: AppLanguage)
}
