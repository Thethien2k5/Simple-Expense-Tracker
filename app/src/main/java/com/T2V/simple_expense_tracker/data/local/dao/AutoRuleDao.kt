package com.T2V.simple_expense_tracker.data.local.dao

import androidx.room.*
import com.T2V.simple_expense_tracker.data.local.entity.AutoRuleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Giao diện truy cập dữ liệu cho các quy tắc tự động.
 */
@Dao
interface AutoRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AutoRuleEntity)

    @Update
    suspend fun updateRule(rule: AutoRuleEntity)

    @Delete
    suspend fun deleteRule(rule: AutoRuleEntity)

    @Query("SELECT * FROM auto_rules")
    fun getAllRules(): Flow<List<AutoRuleEntity>>

    @Query("UPDATE auto_rules SET hitCount = hitCount + 1 WHERE id = :ruleId")
    suspend fun incrementHitCount(ruleId: Long)
}