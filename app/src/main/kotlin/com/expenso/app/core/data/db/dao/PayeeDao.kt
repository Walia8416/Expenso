package com.expenso.app.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expenso.app.core.data.db.entities.PayeeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PayeeDao {

    @Query("SELECT * FROM payee ORDER BY lastUsedAt DESC")
    fun observeAll(): Flow<List<PayeeEntity>>

    @Query("SELECT * FROM payee WHERE vpa = :vpa LIMIT 1")
    suspend fun findByVpa(vpa: String): PayeeEntity?

    @Query("SELECT * FROM payee WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): PayeeEntity?

    @Query("SELECT * FROM payee WHERE contactLookupKey = :lookupKey ORDER BY lastUsedAt DESC")
    fun observeByContact(lookupKey: String): Flow<List<PayeeEntity>>

    @Query("SELECT * FROM payee WHERE contactLookupKey = :lookupKey ORDER BY lastUsedAt DESC")
    suspend fun findByContact(lookupKey: String): List<PayeeEntity>

    @Query(
        """
        SELECT * FROM payee 
        WHERE isPerson = 1
        ORDER BY lastUsedAt DESC
        LIMIT :limit
        """
    )
    fun observeRecentPeople(limit: Int): Flow<List<PayeeEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: PayeeEntity)

    @Update
    suspend fun update(item: PayeeEntity)

    @Query("UPDATE payee SET lastUsedAt = :now WHERE id = :id")
    suspend fun touch(id: String, now: Long)
}
