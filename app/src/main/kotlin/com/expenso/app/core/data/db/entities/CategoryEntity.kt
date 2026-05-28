package com.expenso.app.core.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expenso.app.core.domain.model.Category
import com.expenso.app.core.domain.model.LifestyleGroup

@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val sortOrder: Int,
    val isArchived: Boolean,
    val isDefault: Boolean,
    val lifestyleGroup: String = LifestyleGroup.OTHER.name,
) {
    fun toDomain(): Category = Category(
        id = id,
        name = name,
        emoji = emoji,
        colorHex = colorHex,
        sortOrder = sortOrder,
        isArchived = isArchived,
        isDefault = isDefault,
        lifestyleGroup = LifestyleGroup.fromName(lifestyleGroup),
    )

    companion object {
        fun fromDomain(c: Category): CategoryEntity = CategoryEntity(
            id = c.id,
            name = c.name,
            emoji = c.emoji,
            colorHex = c.colorHex,
            sortOrder = c.sortOrder,
            isArchived = c.isArchived,
            isDefault = c.isDefault,
            lifestyleGroup = c.lifestyleGroup.name,
        )
    }
}
