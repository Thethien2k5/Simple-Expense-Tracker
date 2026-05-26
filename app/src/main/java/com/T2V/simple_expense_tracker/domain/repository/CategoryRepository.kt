package com.T2V.simple_expense_tracker.domain.repository

import com.T2V.simple_expense_tracker.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    suspend fun insertCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    fun getAllCategories(): Flow<List<Category>>
}
