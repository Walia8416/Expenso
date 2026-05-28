package com.expenso.app.core.data.repository

import com.expenso.app.core.data.db.dao.CategoryTotal
import com.expenso.app.core.data.db.dao.DailyCategoryTotal
import com.expenso.app.core.data.db.dao.DailyTotal
import com.expenso.app.core.data.db.dao.ExpenseDao
import com.expenso.app.core.data.db.dao.ExpenseWithRefs
import com.expenso.app.core.data.db.dao.LifestyleTotal
import com.expenso.app.core.data.db.dao.MerchantTotal
import com.expenso.app.core.data.db.dao.PaymentIntentDao
import com.expenso.app.core.data.db.dao.PaymentMethodTotal
import com.expenso.app.core.data.db.entities.ExpenseEntity
import com.expenso.app.core.data.db.entities.PaymentIntentEntity
import com.expenso.app.core.domain.model.Expense
import com.expenso.app.core.domain.model.ExpenseSource
import com.expenso.app.core.domain.model.PaymentMethod
import com.expenso.app.core.domain.model.PaymentStatus
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val paymentIntentDao: PaymentIntentDao,
) {

    fun observeAll(): Flow<List<Expense>> =
        expenseDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeSince(epochMs: Long): Flow<List<Expense>> =
        expenseDao.observeSince(epochMs).map { list -> list.map { it.toDomain() } }

    fun observeInRange(startMs: Long, endMs: Long): Flow<List<Expense>> =
        expenseDao.observeInRange(startMs, endMs).map { list -> list.map { it.toDomain() } }

    fun observePending(): Flow<List<Expense>> =
        expenseDao.observePending().map { list -> list.map { it.toDomain() } }

    suspend fun findById(id: String): Expense? = expenseDao.findById(id)?.toDomain()

    fun observeCategoryTotals(startMs: Long, endMs: Long): Flow<List<CategoryTotal>> =
        expenseDao.observeCategoryTotals(startMs, endMs)

    fun observeTopMerchants(startMs: Long, endMs: Long, limit: Int = 5): Flow<List<MerchantTotal>> =
        expenseDao.observeTopMerchants(startMs, endMs, limit)

    fun observeTotalSpentMinor(startMs: Long, endMs: Long): Flow<Long> =
        expenseDao.observeTotalSpentMinor(startMs, endMs)

    fun observeDailyTotals(
        startMs: Long,
        endMs: Long,
        tzOffsetMs: Long,
    ): Flow<List<DailyTotal>> = expenseDao.observeDailyTotals(startMs, endMs, tzOffsetMs)

    fun observeDailyCategoryTotals(
        startMs: Long,
        endMs: Long,
        tzOffsetMs: Long,
    ): Flow<List<DailyCategoryTotal>> =
        expenseDao.observeDailyCategoryTotals(startMs, endMs, tzOffsetMs)

    fun observeLifestyleTotals(startMs: Long, endMs: Long): Flow<List<LifestyleTotal>> =
        expenseDao.observeLifestyleTotals(startMs, endMs)

    fun observePaymentMethodTotals(
        startMs: Long,
        endMs: Long,
    ): Flow<List<PaymentMethodTotal>> = expenseDao.observePaymentMethodTotals(startMs, endMs)

    fun observeExpenseCount(startMs: Long, endMs: Long): Flow<Int> =
        expenseDao.observeExpenseCount(startMs, endMs)

    fun observeBiggestTxn(startMs: Long, endMs: Long): Flow<Long> =
        expenseDao.observeBiggestTxn(startMs, endMs)

    suspend fun countPotentialDuplicates(
        payeeId: String,
        amountMinor: Long,
        sinceMs: Long,
    ): Int = expenseDao.countPotentialDuplicates(payeeId, amountMinor, sinceMs)

    suspend fun createPendingExpense(
        amountMinor: Long,
        categoryId: String,
        payeeId: String?,
        note: String?,
        source: ExpenseSource,
        intentSessionId: String?,
        now: Long,
    ): String {
        val id = UUID.randomUUID().toString()
        expenseDao.insert(
            ExpenseEntity(
                id = id,
                amountMinor = amountMinor,
                currency = "INR",
                categoryId = categoryId,
                payeeId = payeeId,
                merchantName = null,
                note = note?.takeIf { it.isNotBlank() },
                status = PaymentStatus.PENDING.name,
                source = source.name,
                paymentMethod = PaymentMethod.UPI.name,
                intentSessionId = intentSessionId,
                createdAt = now,
                completedAt = null,
                softDeletedAt = null,
            )
        )
        return id
    }

    suspend fun createCompletedExpense(
        amountMinor: Long,
        categoryId: String,
        paymentMethod: PaymentMethod,
        merchantName: String?,
        note: String?,
        createdAt: Long,
    ): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        expenseDao.insert(
            ExpenseEntity(
                id = id,
                amountMinor = amountMinor,
                currency = "INR",
                categoryId = categoryId,
                payeeId = null,
                merchantName = merchantName?.takeIf { it.isNotBlank() },
                note = note?.takeIf { it.isNotBlank() },
                status = PaymentStatus.COMPLETED.name,
                source = ExpenseSource.MANUAL.name,
                paymentMethod = paymentMethod.name,
                intentSessionId = null,
                createdAt = createdAt,
                completedAt = now,
                softDeletedAt = null,
            )
        )
        return id
    }

    suspend fun insertImported(
        amountMinor: Long,
        categoryId: String,
        merchantName: String?,
        note: String?,
        paymentMethod: PaymentMethod,
        createdAt: Long,
        completedAt: Long?,
    ): String {
        val id = UUID.randomUUID().toString()
        expenseDao.insert(
            ExpenseEntity(
                id = id,
                amountMinor = amountMinor,
                currency = "INR",
                categoryId = categoryId,
                payeeId = null,
                merchantName = merchantName?.takeIf { it.isNotBlank() },
                note = note?.takeIf { it.isNotBlank() },
                status = PaymentStatus.COMPLETED.name,
                source = ExpenseSource.MANUAL.name,
                paymentMethod = paymentMethod.name,
                intentSessionId = null,
                createdAt = createdAt,
                completedAt = completedAt ?: createdAt,
                softDeletedAt = null,
            )
        )
        return id
    }

    suspend fun insertPaymentIntent(
        id: String,
        upiUri: String,
        targetPackage: String?,
        launchedAt: Long,
    ) {
        paymentIntentDao.insert(
            PaymentIntentEntity(
                id = id,
                upiUri = upiUri,
                targetPackage = targetPackage,
                launchedAt = launchedAt,
                resultStatus = null,
                resultRaw = null,
            )
        )
    }

    suspend fun updateIntentResult(id: String, status: String?, raw: String?) {
        paymentIntentDao.updateResult(id, status, raw)
    }

    suspend fun markStatus(expenseId: String, status: PaymentStatus, completedAt: Long?) {
        expenseDao.updateStatus(expenseId, status.name, completedAt)
    }

    suspend fun updateExpense(expense: Expense) {
        val current = expenseDao.findById(expense.id) ?: return
        expenseDao.update(
            current.expense.copy(
                amountMinor = expense.amountMinor,
                categoryId = expense.category.id,
                note = expense.note?.takeIf { it.isNotBlank() },
                merchantName = expense.merchantName?.takeIf { it.isNotBlank() },
                paymentMethod = expense.paymentMethod.name,
            )
        )
    }

    suspend fun softDelete(id: String, now: Long) = expenseDao.softDelete(id, now)
    suspend fun undoSoftDelete(id: String) = expenseDao.undoSoftDelete(id)

    suspend fun expireStalePending(thresholdMs: Long): Int =
        expenseDao.expireStalePending(thresholdMs)

    private fun ExpenseWithRefs.toDomain(): Expense = Expense(
        id = expense.id,
        amountMinor = expense.amountMinor,
        currency = expense.currency,
        category = category.toDomain(),
        payee = payee?.toDomain(),
        merchantName = expense.merchantName,
        note = expense.note,
        status = expense.statusEnum(),
        source = expense.sourceEnum(),
        paymentMethod = expense.paymentMethodEnum(),
        intentSessionId = expense.intentSessionId,
        createdAt = expense.createdAt,
        completedAt = expense.completedAt,
    )
}
