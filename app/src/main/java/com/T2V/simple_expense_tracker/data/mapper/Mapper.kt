package com.T2V.simple_expense_tracker.data.mapper

import com.T2V.simple_expense_tracker.data.local.entity.*
import com.T2V.simple_expense_tracker.domain.model.*

fun BankAccountEntity.toDomain(): BankAccount {
    return BankAccount(
        id = id,
        bankName = bankName,
        accountNumber = accountNumber,
        iconRes = iconRes,
        colorHex = colorHex
    )
}

fun BankAccount.toEntity(): BankAccountEntity {
    return BankAccountEntity(
        id = id,
        bankName = bankName,
        accountNumber = accountNumber,
        iconRes = iconRes,
        colorHex = colorHex
    )
}

fun TransactionEntity.toDomain(): Transaction {
    return Transaction(
        id = id,
        rawNotificationId = rawNotificationId,
        bankAccountId = bankAccountId,
        amount = amount,
        counterparty = counterparty,
        content = content,
        timestamp = timestamp
    )
}

fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = id,
        rawNotificationId = rawNotificationId,
        bankAccountId = bankAccountId,
        amount = amount,
        counterparty = counterparty,
        content = content,
        timestamp = timestamp
    )
}

fun RawNotificationEntity.toDomain(): RawNotification {
    return RawNotification(
        id = id,
        bankName = bankName,
        fullContent = fullContent,
        receivedAt = receivedAt,
        isProcessed = isProcessed
    )
}

fun RawNotification.toEntity(): RawNotificationEntity {
    return RawNotificationEntity(
        id = id,
        bankName = bankName,
        fullContent = fullContent,
        receivedAt = receivedAt,
        isProcessed = isProcessed
    )
}

