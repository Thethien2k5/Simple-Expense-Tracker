package com.T2V.simple_expense_tracker.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.T2V.simple_expense_tracker.data.local.dao.BankAccountDao
import com.T2V.simple_expense_tracker.data.local.dao.TransactionDao
import com.T2V.simple_expense_tracker.data.local.entity.BankAccountEntity
import com.T2V.simple_expense_tracker.data.local.entity.TransactionEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "mock_seeder")

@Singleton
class MockDataSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bankAccountDao: BankAccountDao,
    private val transactionDao: TransactionDao
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    private val seededKey = booleanPreferencesKey("mock_data_seeded")

    fun seedIfNeeded(onComplete: (() -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = context.dataStore.data.first()
            if (prefs[seededKey] == true) {
                onComplete?.invoke()
                return@launch
            }
            try {
                val lines = context.assets.open("mock_data.txt")
                    .bufferedReader()
                    .readLines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }

                var bankIdMap = mutableMapOf<String, Long>()
                for (line in lines) {
                    val parts = line.split("|").map { it.trim() }
                    if (parts.isEmpty() || parts[0].uppercase() == "BANK") continue
                    if (parts[0].uppercase() == "TX") continue
                }

                for (line in lines) {
                    val parts = line.split("|").map { it.trim() }
                    if (parts.isEmpty() || parts.size < 2) continue
                    val type = parts[0].uppercase()
                    if (type == "BANK" && parts.size >= 6) {
                        val entity = BankAccountEntity(
                            bankName = parts[1],
                            accountNumber = parts[2],
                            iconRes = parts[3],
                            colorHex = parts[4],
                            balance = parts[5].toDouble()
                        )
                        val id = bankAccountDao.insertBankAccount(entity)
                        bankIdMap[parts[1]] = id
                    } else if (type == "TX" && parts.size >= 6) {
                        val bankName = parts[1]
                        val bankId = bankIdMap[bankName] ?: continue
                        val ts = try { dateFormat.parse(parts[5])!!.time } catch (e: Exception) { System.currentTimeMillis() }
                        val entity = TransactionEntity(
                            rawNotificationId = 0,
                            bankAccountId = bankId,
                            amount = parts[2].toDouble(),
                            counterparty = parts[3],
                            content = parts[4],
                            timestamp = ts
                        )
                        transactionDao.insertTransaction(entity)
                    }
                }

                context.dataStore.edit { it[seededKey] = true }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onComplete?.invoke()
        }
    }
}
