package com.expenso.app.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.expenso.app.core.data.db.entities.PaymentIntentEntity

@Dao
interface PaymentIntentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PaymentIntentEntity)

    @Query("SELECT * FROM payment_intent WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): PaymentIntentEntity?

    @Query("UPDATE payment_intent SET resultStatus = :status, resultRaw = :raw WHERE id = :id")
    suspend fun updateResult(id: String, status: String?, raw: String?)
}
