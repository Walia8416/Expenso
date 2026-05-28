package com.expenso.app.core.data.repository

import com.expenso.app.core.data.db.dao.IncomeDao
import com.expenso.app.core.data.db.dao.SourceTotal
import com.expenso.app.core.data.db.entities.IncomeEntity
import com.expenso.app.core.domain.model.Income
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class IncomeRepository @Inject constructor(
    private val incomeDao: IncomeDao,
) {

    fun observeAll(): Flow<List<Income>> =
        incomeDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeInRange(startMs: Long, endMs: Long): Flow<List<Income>> =
        incomeDao.observeInRange(startMs, endMs).map { list -> list.map { it.toDomain() } }

    fun observeTotalInRange(startMs: Long, endMs: Long): Flow<Long> =
        incomeDao.observeTotalInRange(startMs, endMs)

    fun observeSourceTotals(startMs: Long, endMs: Long): Flow<List<SourceTotal>> =
        incomeDao.observeSourceTotals(startMs, endMs)

    suspend fun recentSources(limit: Int = 8): List<String> =
        incomeDao.recentSources(limit)

    suspend fun findById(id: String): Income? = incomeDao.findById(id)?.toDomain()

    suspend fun add(
        amountMinor: Long,
        source: String,
        description: String?,
        note: String?,
        createdAt: Long,
    ): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        incomeDao.insert(
            IncomeEntity(
                id = id,
                amountMinor = amountMinor,
                currency = "INR",
                source = source.trim(),
                description = description?.takeIf { it.isNotBlank() },
                note = note?.takeIf { it.isNotBlank() },
                createdAt = createdAt,
                recordedAt = now,
                softDeletedAt = null,
            )
        )
        return id
    }

    suspend fun update(income: Income) {
        val existing = incomeDao.findById(income.id) ?: return
        incomeDao.update(
            existing.copy(
                amountMinor = income.amountMinor,
                source = income.source.trim(),
                description = income.description?.takeIf { it.isNotBlank() },
                note = income.note?.takeIf { it.isNotBlank() },
                createdAt = income.createdAt,
            )
        )
    }

    suspend fun softDelete(id: String, now: Long) = incomeDao.softDelete(id, now)
    suspend fun undoSoftDelete(id: String) = incomeDao.undoSoftDelete(id)
}
