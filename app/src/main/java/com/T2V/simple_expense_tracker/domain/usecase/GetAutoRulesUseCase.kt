package com.T2V.simple_expense_tracker.domain.usecase

import com.T2V.simple_expense_tracker.domain.model.AutoRule
import com.T2V.simple_expense_tracker.domain.repository.AutoRuleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAutoRulesUseCase @Inject constructor(
    private val repository: AutoRuleRepository
) {
    operator fun invoke(): Flow<List<AutoRule>> {
        return repository.getAllRules()
    }
}
