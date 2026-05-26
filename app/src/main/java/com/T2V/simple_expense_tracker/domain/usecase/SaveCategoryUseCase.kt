package com.T2V.simple_expense_tracker.domain.usecase

import com.T2V.simple_expense_tracker.domain.model.Category
import com.T2V.simple_expense_tracker.domain.repository.CategoryRepository
import javax.inject.Inject

/**
 * Lưu danh mục mới hoặc cập nhật danh mục hiện tại.
 * Trả về ID của danh mục vừa tạo (khi insert mới).
 */
class SaveCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(category: Category): Long {
        return if (category.id == 0L) {
            repository.insertCategory(category)
        } else {
            repository.updateCategory(category)
            category.id
        }
    }
}
