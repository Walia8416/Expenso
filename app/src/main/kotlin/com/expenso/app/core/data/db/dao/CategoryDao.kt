package com.expenso.app.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.expenso.app.core.data.db.entities.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM category WHERE isArchived = 0 ORDER BY sortOrder ASC")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM category")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: CategoryEntity)

    @Update
    suspend fun update(item: CategoryEntity)

    @Query("UPDATE category SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: String)

    @Query("UPDATE category SET isArchived = 0 WHERE id = :id")
    suspend fun unarchive(id: String)

    @Query("UPDATE category SET sortOrder = :order WHERE id = :id")
    suspend fun setSortOrder(id: String, order: Int)

    @Transaction
    suspend fun reorder(ids: List<String>) {
        ids.forEachIndexed { index, id -> setSortOrder(id, index) }
    }

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM category")
    suspend fun maxSortOrder(): Int
}
