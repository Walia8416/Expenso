package com.expenso.app.core.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.expenso.app.core.domain.model.ExpenseSource
import com.expenso.app.core.domain.model.PaymentMethod
import com.expenso.app.core.domain.model.PaymentStatus

@Entity(
    tableName = "expense",
    indices = [
        Index("createdAt"),
        Index("categoryId"),
        Index("payeeId"),
        Index("status"),
        Index("paymentMethod"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = PayeeEntity::class,
            parentColumns = ["id"],
            childColumns = ["payeeId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val amountMinor: Long,
    val currency: String,
    val categoryId: String,
    val payeeId: String?,
    val merchantName: String?,
    val note: String?,
    val status: String,
    val source: String,
    val paymentMethod: String = PaymentMethod.UPI.name,
    val intentSessionId: String?,
    val createdAt: Long,
    val completedAt: Long?,
    val softDeletedAt: Long?,
) {
    fun statusEnum(): PaymentStatus = PaymentStatus.valueOf(status)
    fun sourceEnum(): ExpenseSource = ExpenseSource.valueOf(source)
    fun paymentMethodEnum(): PaymentMethod = PaymentMethod.fromName(paymentMethod)
}
