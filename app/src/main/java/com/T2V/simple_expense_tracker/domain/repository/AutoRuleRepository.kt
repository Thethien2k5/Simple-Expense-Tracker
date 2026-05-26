package com.T2V.simple_expense_tracker.domain.repository

import com.T2V.simple_expense_tracker.domain.model.AutoRule
import kotlinx.coroutines.flow.Flow

interface AutoRuleRepository {
    suspend fun insertRule(rule: AutoRule)
    suspend fun updateRule(rule: AutoRule)
    suspend fun deleteRule(rule: AutoRule)
    fun getAllRules(): Flow<List<AutoRule>>
    suspend fun incrementHitCount(ruleId: Long)
}
