package com.T2V.simple_expense_tracker.domain.usecase

import com.T2V.simple_expense_tracker.domain.model.Category
import com.T2V.simple_expense_tracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    operator fun invoke(): Flow<List<Category>> {
        return repository.getAllCategories()
    }
}
