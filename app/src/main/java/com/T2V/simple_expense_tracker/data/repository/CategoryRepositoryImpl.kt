package com.T2V.simple_expense_tracker.data.repository

import com.T2V.simple_expense_tracker.data.local.dao.CategoryDao
import com.T2V.simple_expense_tracker.data.mapper.toDomain
import com.T2V.simple_expense_tracker.data.mapper.toEntity
import com.T2V.simple_expense_tracker.domain.model.Category
import com.T2V.simple_expense_tracker.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override suspend fun insertCategory(category: Category): Long {
        return categoryDao.insertCategory(category.toEntity())
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.toEntity())
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category.toEntity())
    }

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
