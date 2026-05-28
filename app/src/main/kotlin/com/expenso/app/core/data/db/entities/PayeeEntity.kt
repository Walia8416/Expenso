package com.expenso.app.core.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.expenso.app.core.domain.model.Payee

@Entity(
    tableName = "payee",
    indices = [
        Index(value = ["vpa"], unique = true),
        Index("contactLookupKey"),
        Index("phoneNumber"),
    ],
)
data class PayeeEntity(
    @PrimaryKey val id: String,
    val vpa: String,
    val displayName: String,
    val parsedName: String?,
    val merchantCode: String?,
    val suggestedCategoryId: String?,
    val contactLookupKey: String? = null,
    val phoneNumber: String? = null,
    val isPerson: Boolean = false,
    val firstSeenAt: Long,
    val lastUsedAt: Long,
) {
    fun toDomain(): Payee = Payee(
        id = id,
        vpa = vpa,
        displayName = displayName,
        parsedName = parsedName,
        merchantCode = merchantCode,
        suggestedCategoryId = suggestedCategoryId,
        contactLookupKey = contactLookupKey,
        phoneNumber = phoneNumber,
        isPerson = isPerson,
        firstSeenAt = firstSeenAt,
        lastUsedAt = lastUsedAt,
    )
}
