package com.T2V.simple_expense_tracker.data.repository

import com.T2V.simple_expense_tracker.data.local.dao.AutoRuleDao
import com.T2V.simple_expense_tracker.data.mapper.toDomain
import com.T2V.simple_expense_tracker.data.mapper.toEntity
import com.T2V.simple_expense_tracker.domain.model.AutoRule
import com.T2V.simple_expense_tracker.domain.repository.AutoRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AutoRuleRepositoryImpl @Inject constructor(
    private val autoRuleDao: AutoRuleDao
) : AutoRuleRepository {

    override suspend fun insertRule(rule: AutoRule) {
        autoRuleDao.insertRule(rule.toEntity())
    }

    override suspend fun updateRule(rule: AutoRule) {
        autoRuleDao.updateRule(rule.toEntity())
    }

    override suspend fun deleteRule(rule: AutoRule) {
        autoRuleDao.deleteRule(rule.toEntity())
    }

    override fun getAllRules(): Flow<List<AutoRule>> {
        return autoRuleDao.getAllRules().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun incrementHitCount(ruleId: Long) {
        autoRuleDao.incrementHitCount(ruleId)
    }
}
