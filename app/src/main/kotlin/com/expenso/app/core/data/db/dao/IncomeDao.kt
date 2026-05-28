package com.expenso.app.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expenso.app.core.data.db.entities.IncomeEntity
import kotlinx.coroutines.flow.Flow

data class SourceTotal(
    val source: String,
    val total: Long,
    val count: Int,
)

@Dao
interface IncomeDao {

    @Query(
        """
        SELECT * FROM income 
        WHERE softDeletedAt IS NULL
        ORDER BY createdAt DESC
        """
    )
    fun observeAll(): Flow<List<IncomeEntity>>

    @Query(
        """
        SELECT * FROM income
        WHERE softDeletedAt IS NULL
          AND createdAt >= :startMs
          AND createdAt < :endMs
        ORDER BY createdAt DESC
        """
    )
    fun observeInRange(startMs: Long, endMs: Long): Flow<List<IncomeEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(amountMinor), 0) FROM income
        WHERE softDeletedAt IS NULL
          AND createdAt >= :startMs
          AND createdAt < :endMs
        """
    )
    fun observeTotalInRange(startMs: Long, endMs: Long): Flow<Long>

    @Query(
        """
        SELECT source as source,
               SUM(amountMinor) as total,
               COUNT(*) as count
        FROM income
        WHERE softDeletedAt IS NULL
          AND createdAt >= :startMs
          AND createdAt < :endMs
        GROUP BY source
        ORDER BY total DESC
        """
    )
    fun observeSourceTotals(startMs: Long, endMs: Long): Flow<List<SourceTotal>>

    @Query(
        """
        SELECT DISTINCT source FROM income
        WHERE softDeletedAt IS NULL
        ORDER BY recordedAt DESC
        LIMIT :limit
        """
    )
    suspend fun recentSources(limit: Int): List<String>

    @Query("SELECT * FROM income WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): IncomeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: IncomeEntity)

    @Update
    suspend fun update(item: IncomeEntity)

    @Query("UPDATE income SET softDeletedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE income SET softDeletedAt = NULL WHERE id = :id")
    suspend fun undoSoftDelete(id: String)
}
