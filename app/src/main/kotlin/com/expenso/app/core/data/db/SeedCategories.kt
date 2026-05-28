package com.expenso.app.core.data.db

import com.expenso.app.core.data.db.entities.CategoryEntity
import com.expenso.app.core.domain.model.LifestyleGroup

object SeedCategories {
    val defaults: List<CategoryEntity> = listOf(
        seed("groceries", "Groceries", "\uD83D\uDED2", "#2DAE85", 0, LifestyleGroup.ESSENTIAL),
        seed("bills", "Bills", "\uD83D\uDCC4", "#6A4FE2", 1, LifestyleGroup.ESSENTIAL),
        seed("rent", "Rent", "\uD83C\uDFE0", "#8B5E3C", 2, LifestyleGroup.ESSENTIAL),
        seed("transport", "Transport", "\uD83D\uDE95", "#3C78D8", 3, LifestyleGroup.ESSENTIAL),
        seed("health", "Health", "\uD83E\uDE7A", "#4FBDE2", 4, LifestyleGroup.ESSENTIAL),
        seed("food", "Food", "\uD83C\uDF5B", "#E26A4F", 5, LifestyleGroup.LIFESTYLE),
        seed("shopping", "Shopping", "\uD83D\uDECD", "#D84FA6", 6, LifestyleGroup.LIFESTYLE),
        seed("entertainment", "Fun", "\uD83C\uDFAC", "#F3B23C", 7, LifestyleGroup.LIFESTYLE),
        seed("travel", "Travel", "\u2708\uFE0F", "#4DB6AC", 8, LifestyleGroup.LIFESTYLE),
        seed("fitness", "Fitness", "\uD83C\uDFCB\uFE0F", "#FF8A65", 9, LifestyleGroup.GROWTH),
        seed("education", "Education", "\uD83D\uDCDA", "#7E57C2", 10, LifestyleGroup.GROWTH),
        seed("subscriptions", "Subscriptions", "\uD83D\uDCF1", "#26A69A", 11, LifestyleGroup.GROWTH),
        seed("transfer", "Transfer", "\uD83D\uDD01", "#5E5E59", 12, LifestyleGroup.OTHER),
        seed("other", "Other", "\u2022", "#9A9A93", 13, LifestyleGroup.OTHER),
    )

    private fun seed(
        id: String,
        name: String,
        emoji: String,
        color: String,
        order: Int,
        group: LifestyleGroup,
    ) = CategoryEntity(
        id = id,
        name = name,
        emoji = emoji,
        colorHex = color,
        sortOrder = order,
        isArchived = false,
        isDefault = true,
        lifestyleGroup = group.name,
    )

    const val FALLBACK_ID = "other"
}
