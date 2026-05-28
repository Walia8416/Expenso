package com.expenso.app.core.data.repository

import com.expenso.app.core.data.db.SeedCategories
import com.expenso.app.core.data.db.dao.CategoryDao
import com.expenso.app.core.data.db.entities.CategoryEntity
import com.expenso.app.core.domain.model.Category
import com.expenso.app.core.domain.model.LifestyleGroup
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CategoryRepository @Inject constructor(
    private val dao: CategoryDao,
) {

    fun observeActive(): Flow<List<Category>> =
        dao.observeActive().map { list -> list.map(CategoryEntity::toDomain) }

    fun observeAll(): Flow<List<Category>> =
        dao.observeAll().map { list -> list.map(CategoryEntity::toDomain) }

    suspend fun findById(id: String): Category? = dao.findById(id)?.toDomain()

    suspend fun ensureSeeded() {
        if (dao.count() == 0) {
            dao.insertAll(SeedCategories.defaults)
        }
    }

    suspend fun update(category: Category) {
        dao.update(CategoryEntity.fromDomain(category))
    }

    suspend fun create(
        name: String,
        emoji: String,
        colorHex: String,
        lifestyleGroup: LifestyleGroup,
    ): Category {
        val id = slugify(name) + "_" + UUID.randomUUID().toString().take(6)
        val nextOrder = dao.maxSortOrder() + 1
        val entity = CategoryEntity(
            id = id,
            name = name,
            emoji = emoji,
            colorHex = colorHex,
            sortOrder = nextOrder,
            isArchived = false,
            isDefault = false,
            lifestyleGroup = lifestyleGroup.name,
        )
        dao.insert(entity)
        return entity.toDomain()
    }

    suspend fun reorder(ids: List<String>) = dao.reorder(ids)
    suspend fun archive(id: String) = dao.archive(id)
    suspend fun unarchive(id: String) = dao.unarchive(id)

    private fun slugify(s: String): String =
        s.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "cat" }
}
