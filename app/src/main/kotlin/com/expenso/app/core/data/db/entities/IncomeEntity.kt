package com.expenso.app.core.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.expenso.app.core.domain.model.Income

@Entity(
    tableName = "income",
    indices = [
        Index("createdAt"),
        Index("source"),
    ],
)
data class IncomeEntity(
    @PrimaryKey val id: String,
    val amountMinor: Long,
    val currency: String,
    val source: String,
    val description: String?,
    val note: String?,
    val createdAt: Long,
    val recordedAt: Long,
    val softDeletedAt: Long?,
) {
    fun toDomain(): Income = Income(
        id = id,
        amountMinor = amountMinor,
        currency = currency,
        source = source,
        description = description,
        note = note,
        createdAt = createdAt,
        recordedAt = recordedAt,
    )
}
