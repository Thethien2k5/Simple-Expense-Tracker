package com.T2V.simple_expense_tracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.T2V.simple_expense_tracker.domain.repository.AppLanguage
import com.T2V.simple_expense_tracker.domain.repository.LanguageRepository
import com.T2V.simple_expense_tracker.domain.repository.ThemeRepository
import com.T2V.simple_expense_tracker.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

data class SettingsUiState(
    val isLoading: Boolean = false,
    val currentTheme: AppTheme = AppTheme.EMERALD,
    val currentLanguage: AppLanguage = AppLanguage.VIETNAMESE,
    val aboutInfo: Map<String, String>? = null
)

@HiltViewModel //Nó giống như `@Singleton`, nhưng dành riêng cho ViewModel
class SettingsViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
    private val languageRepository: LanguageRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            themeRepository.selectedTheme.collect { theme ->
                _uiState.value = _uiState.value.copy(currentTheme = theme)
            }
        }
        viewModelScope.launch {
            languageRepository.selectedLanguage.collect { language ->
                _uiState.value = _uiState.value.copy(currentLanguage = language)
            }
        }
    }

    fun onThemeSelected(theme: AppTheme) {
        viewModelScope.launch {
            themeRepository.setTheme(theme)
        }
    }

    suspend fun onLanguageSelected(language: AppLanguage) {
        languageRepository.setLanguage(language)
    }

    fun loadAboutInfo() {
        if (_uiState.value.aboutInfo != null) return
        viewModelScope.launch {
            try {
                val jsonString = context.assets.open("in_for.json").bufferedReader().use { it.readText() }
                val jsonObject = org.json.JSONObject(jsonString)
                val map = mutableMapOf<String, String>()
                val keys = jsonObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = jsonObject.getString(key)
                }
                _uiState.value = _uiState.value.copy(aboutInfo = map)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
