package com.expenso.app.core.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_intent")
data class PaymentIntentEntity(
    @PrimaryKey val id: String,
    val upiUri: String,
    val targetPackage: String?,
    val launchedAt: Long,
    val resultStatus: String?,
    val resultRaw: String?,
)
