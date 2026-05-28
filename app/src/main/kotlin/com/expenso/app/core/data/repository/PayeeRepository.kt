package com.expenso.app.core.data.repository

import com.expenso.app.core.data.db.dao.PayeeDao
import com.expenso.app.core.data.db.entities.PayeeEntity
import com.expenso.app.core.domain.model.Payee
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class PayeeRepository @Inject constructor(
    private val dao: PayeeDao,
) {

    fun observeAll(): Flow<List<Payee>> =
        dao.observeAll().map { list -> list.map(PayeeEntity::toDomain) }

    suspend fun findByVpa(vpa: String): Payee? =
        dao.findByVpa(vpa.lowercase())?.toDomain()

    suspend fun findById(id: String): Payee? = dao.findById(id)?.toDomain()

    fun observeByContact(lookupKey: String): Flow<List<Payee>> =
        dao.observeByContact(lookupKey).map { list -> list.map(PayeeEntity::toDomain) }

    suspend fun findByContact(lookupKey: String): List<Payee> =
        dao.findByContact(lookupKey).map(PayeeEntity::toDomain)

    fun observeRecentPeople(limit: Int = 10): Flow<List<Payee>> =
        dao.observeRecentPeople(limit).map { list -> list.map(PayeeEntity::toDomain) }

    /**
     * Returns an existing Payee for the given VPA or creates a new one.
     * If the payee already exists, updates `parsedName` and `lastUsedAt`.
     */
    suspend fun upsertForPayment(
        vpa: String,
        parsedName: String?,
        merchantCode: String?,
        now: Long,
    ): Payee {
        val normalized = vpa.lowercase()
        val existing = dao.findByVpa(normalized)
        if (existing != null) {
            val updated = existing.copy(
                parsedName = parsedName ?: existing.parsedName,
                merchantCode = merchantCode ?: existing.merchantCode,
                lastUsedAt = now,
            )
            dao.update(updated)
            return updated.toDomain()
        }
        val entity = PayeeEntity(
            id = UUID.randomUUID().toString(),
            vpa = normalized,
            displayName = parsedName?.takeIf { it.isNotBlank() } ?: normalized,
            parsedName = parsedName,
            merchantCode = merchantCode,
            suggestedCategoryId = null,
            firstSeenAt = now,
            lastUsedAt = now,
        )
        dao.insert(entity)
        return entity.toDomain()
    }

    suspend fun upsertContactPayee(
        vpa: String,
        displayName: String,
        contactLookupKey: String?,
        phoneNumber: String?,
        now: Long,
    ): Payee {
        val normalized = vpa.lowercase()
        val existing = dao.findByVpa(normalized)
        if (existing != null) {
            val updated = existing.copy(
                displayName = displayName.ifBlank { existing.displayName },
                parsedName = displayName.ifBlank { existing.parsedName },
                contactLookupKey = contactLookupKey ?: existing.contactLookupKey,
                phoneNumber = phoneNumber ?: existing.phoneNumber,
                isPerson = true,
                lastUsedAt = now,
            )
            dao.update(updated)
            return updated.toDomain()
        }
        val entity = PayeeEntity(
            id = UUID.randomUUID().toString(),
            vpa = normalized,
            displayName = displayName.ifBlank { normalized },
            parsedName = displayName,
            merchantCode = null,
            suggestedCategoryId = null,
            contactLookupKey = contactLookupKey,
            phoneNumber = phoneNumber,
            isPerson = true,
            firstSeenAt = now,
            lastUsedAt = now,
        )
        dao.insert(entity)
        return entity.toDomain()
    }

    suspend fun touch(id: String, now: Long) = dao.touch(id, now)
}
