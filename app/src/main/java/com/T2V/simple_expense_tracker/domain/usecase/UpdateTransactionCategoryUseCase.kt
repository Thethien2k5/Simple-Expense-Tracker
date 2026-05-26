package com.T2V.simple_expense_tracker.domain.usecase

import com.T2V.simple_expense_tracker.domain.model.Transaction
import com.T2V.simple_expense_tracker.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Cập nhật danh mục cho giao dịch khi người dùng phân loại thủ công.
 * Dùng khi người dùng chọn danh mục cho giao dịch ở "Chờ Xử Lý".
 */
class UpdateTransactionCategoryUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction, newCategoryId: Long) {
        repository.updateTransaction(transaction.copy(categoryId = newCategoryId))
    }
}
