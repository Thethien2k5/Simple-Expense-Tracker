package com.T2V.simple_expense_tracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.T2V.simple_expense_tracker.domain.model.Category
import com.T2V.simple_expense_tracker.domain.repository.CategoryRepository
import com.T2V.simple_expense_tracker.domain.usecase.GetCategoriesUseCase
import com.T2V.simple_expense_tracker.domain.usecase.SaveCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel cho panel "Tùy chỉnh" (drawer trái).
 * Quản lý danh sách Category (loại hình giao dịch) và cài đặt ứng dụng.
 */
data class SettingsUiState(
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val showAddCategoryDialog: Boolean = false,
    val newCategoryName: String = "",
    val newCategoryIcon: String = "category",
    val newCategoryColor: String = "#4EDEA3"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    getCategoriesUseCase: GetCategoriesUseCase,
    private val saveCategoryUseCase: SaveCategoryUseCase,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getCategoriesUseCase().collect { categories ->
                _uiState.update {
                    it.copy(isLoading = false, categories = categories)
                }
            }
        }
    }

    /** Hiển thị/ẩn dialog thêm danh mục */
    fun toggleAddCategoryDialog(show: Boolean) {
        _uiState.update { it.copy(showAddCategoryDialog = show) }
    }

    /** Cập nhật tên danh mục mới */
    fun updateNewCategoryName(name: String) {
        _uiState.update { it.copy(newCategoryName = name) }
    }

    /** Cập nhật icon danh mục mới */
    fun updateNewCategoryIcon(icon: String) {
        _uiState.update { it.copy(newCategoryIcon = icon) }
    }

    /** Cập nhật màu danh mục mới */
    fun updateNewCategoryColor(color: String) {
        _uiState.update { it.copy(newCategoryColor = color) }
    }

    /** Lưu danh mục mới vào DB */
    fun saveNewCategory() {
        val state = _uiState.value
        if (state.newCategoryName.isBlank()) return

        viewModelScope.launch {
            saveCategoryUseCase(
                Category(
                    name = state.newCategoryName.trim(),
                    iconRes = state.newCategoryIcon,
                    colorHex = state.newCategoryColor
                )
            )
            // Reset form sau khi lưu
            _uiState.update {
                it.copy(
                    showAddCategoryDialog = false,
                    newCategoryName = "",
                    newCategoryIcon = "category",
                    newCategoryColor = "#4EDEA3"
                )
            }
        }
    }

    /** Xóa danh mục */
    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }
}
