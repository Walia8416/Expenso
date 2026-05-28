package com.expenso.app.core.data.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.expenso.app.core.data.db.entities.CategoryEntity
import com.expenso.app.core.data.db.entities.ExpenseEntity
import com.expenso.app.core.data.db.entities.PayeeEntity
import kotlinx.coroutines.flow.Flow

data class ExpenseWithRefs(
    @Embedded val expense: ExpenseEntity,

    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: CategoryEntity,

    @Relation(parentColumn = "payeeId", entityColumn = "id")
    val payee: PayeeEntity?,
)

data class CategoryTotal(
    val categoryId: String,
    val total: Long,
    val count: Int,
)

data class MerchantTotal(
    val payeeId: String,
    val vpa: String,
    val displayName: String,
    val total: Long,
    val count: Int,
)

data class DailyTotal(
    val dayIndex: Long,
    val total: Long,
    val count: Int,
)

data class DailyCategoryTotal(
    val dayIndex: Long,
    val categoryId: String,
    val total: Long,
    val count: Int,
)

data class LifestyleTotal(
    val lifestyleGroup: String,
    val total: Long,
    val count: Int,
)

data class PaymentMethodTotal(
    val paymentMethod: String,
    val total: Long,
    val count: Int,
)

@Dao
interface ExpenseDao {

    @Transaction
    @Query(
        """
        SELECT * FROM expense 
        WHERE softDeletedAt IS NULL
        ORDER BY createdAt DESC
        """
    )
    fun observeAll(): Flow<List<ExpenseWithRefs>>

    @Transaction
    @Query(
        """
        SELECT * FROM expense 
        WHERE softDeletedAt IS NULL AND createdAt >= :sinceEpochMs
        ORDER BY createdAt DESC
        """
    )
    fun observeSince(sinceEpochMs: Long): Flow<List<ExpenseWithRefs>>

    @Transaction
    @Query(
        """
        SELECT * FROM expense 
        WHERE softDeletedAt IS NULL 
          AND createdAt >= :startMs 
          AND createdAt < :endMs
        ORDER BY createdAt DESC
        """
    )
    fun observeInRange(startMs: Long, endMs: Long): Flow<List<ExpenseWithRefs>>

    @Transaction
    @Query(
        """
        SELECT * FROM expense
        WHERE softDeletedAt IS NULL AND status = 'PENDING'
        ORDER BY createdAt DESC
        """
    )
    fun observePending(): Flow<List<ExpenseWithRefs>>

    @Transaction
    @Query("SELECT * FROM expense WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ExpenseWithRefs?

    @Query(
        """
        SELECT COUNT(*) FROM expense 
        WHERE softDeletedAt IS NULL 
          AND payeeId = :payeeId 
          AND amountMinor = :amountMinor 
          AND createdAt >= :sinceMs 
          AND status IN ('PENDING', 'COMPLETED')
        """
    )
    suspend fun countPotentialDuplicates(
        payeeId: String,
        amountMinor: Long,
        sinceMs: Long,
    ): Int

    @Query(
        """
        SELECT categoryId as categoryId, 
               SUM(amountMinor) as total, 
               COUNT(*) as count
        FROM expense
        WHERE softDeletedAt IS NULL 
          AND status = 'COMPLETED'
          AND createdAt >= :startMs 
          AND createdAt < :endMs
        GROUP BY categoryId
        ORDER BY total DESC
        """
    )
    fun observeCategoryTotals(startMs: Long, endMs: Long): Flow<List<CategoryTotal>>

    @Query(
        """
        SELECT p.id as payeeId, p.vpa as vpa, p.displayName as displayName,
               SUM(e.amountMinor) as total, COUNT(*) as count
        FROM expense e
        INNER JOIN payee p ON p.id = e.payeeId
        WHERE e.softDeletedAt IS NULL 
          AND e.status = 'COMPLETED'
          AND e.createdAt >= :startMs 
          AND e.createdAt < :endMs
        GROUP BY p.id
        ORDER BY total DESC
        LIMIT :limit
        """
    )
    fun observeTopMerchants(
        startMs: Long,
        endMs: Long,
        limit: Int,
    ): Flow<List<MerchantTotal>>

    @Query(
        """
        SELECT COALESCE(SUM(amountMinor), 0) FROM expense
        WHERE softDeletedAt IS NULL 
          AND status = 'COMPLETED'
          AND createdAt >= :startMs 
          AND createdAt < :endMs
        """
    )
    fun observeTotalSpentMinor(startMs: Long, endMs: Long): Flow<Long>

    @Query(
        """
        SELECT ((createdAt + :tzOffsetMs) / 86400000) as dayIndex,
               SUM(amountMinor) as total,
               COUNT(*) as count
        FROM expense
        WHERE softDeletedAt IS NULL 
          AND status = 'COMPLETED'
          AND createdAt >= :startMs 
          AND createdAt < :endMs
        GROUP BY dayIndex
        ORDER BY dayIndex ASC
        """
    )
    fun observeDailyTotals(
        startMs: Long,
        endMs: Long,
        tzOffsetMs: Long,
    ): Flow<List<DailyTotal>>

    @Query(
        """
        SELECT ((createdAt + :tzOffsetMs) / 86400000) as dayIndex,
               categoryId as categoryId,
               SUM(amountMinor) as total,
               COUNT(*) as count
        FROM expense
        WHERE softDeletedAt IS NULL 
          AND status = 'COMPLETED'
          AND createdAt >= :startMs 
          AND createdAt < :endMs
        GROUP BY dayIndex, categoryId
        ORDER BY dayIndex ASC
        """
    )
    fun observeDailyCategoryTotals(
        startMs: Long,
        endMs: Long,
        tzOffsetMs: Long,
    ): Flow<List<DailyCategoryTotal>>

    @Query(
        """
        SELECT c.lifestyleGroup as lifestyleGroup,
               SUM(e.amountMinor) as total,
               COUNT(*) as count
        FROM expense e
        INNER JOIN category c ON c.id = e.categoryId
        WHERE e.softDeletedAt IS NULL 
          AND e.status = 'COMPLETED'
          AND e.createdAt >= :startMs 
          AND e.createdAt < :endMs
        GROUP BY c.lifestyleGroup
        ORDER BY total DESC
        """
    )
    fun observeLifestyleTotals(startMs: Long, endMs: Long): Flow<List<LifestyleTotal>>

    @Query(
        """
        SELECT paymentMethod as paymentMethod,
               SUM(amountMinor) as total,
               COUNT(*) as count
        FROM expense
        WHERE softDeletedAt IS NULL 
          AND status = 'COMPLETED'
          AND createdAt >= :startMs 
          AND createdAt < :endMs
        GROUP BY paymentMethod
        ORDER BY total DESC
        """
    )
    fun observePaymentMethodTotals(
        startMs: Long,
        endMs: Long,
    ): Flow<List<PaymentMethodTotal>>

    @Query(
        """
        SELECT COUNT(*) FROM expense
        WHERE softDeletedAt IS NULL 
          AND status = 'COMPLETED'
          AND createdAt >= :startMs 
          AND createdAt < :endMs
        """
    )
    fun observeExpenseCount(startMs: Long, endMs: Long): Flow<Int>

    @Query(
        """
        SELECT COALESCE(MAX(amountMinor), 0) FROM expense
        WHERE softDeletedAt IS NULL 
          AND status = 'COMPLETED'
          AND createdAt >= :startMs 
          AND createdAt < :endMs
        """
    )
    fun observeBiggestTxn(startMs: Long, endMs: Long): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ExpenseEntity)

    @Update
    suspend fun update(item: ExpenseEntity)

    @Query("UPDATE expense SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, completedAt: Long?)

    @Query("UPDATE expense SET softDeletedAt = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE expense SET softDeletedAt = NULL WHERE id = :id")
    suspend fun undoSoftDelete(id: String)

    @Query(
        """
        UPDATE expense 
        SET status = 'EXPIRED' 
        WHERE status = 'PENDING' AND createdAt < :thresholdMs
        """
    )
    suspend fun expireStalePending(thresholdMs: Long): Int
}
