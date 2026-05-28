package com.expenso.app.core.domain.model

data class Category(
    val id: String,
    val name: String,
    val emoji: String,
    val colorHex: String,
    val sortOrder: Int,
    val isArchived: Boolean = false,
    val isDefault: Boolean = false,
    val lifestyleGroup: LifestyleGroup = LifestyleGroup.OTHER,
)

enum class LifestyleGroup(val displayName: String) {
    ESSENTIAL("Essential"),
    LIFESTYLE("Lifestyle"),
    GROWTH("Growth"),
    OTHER("Other");

    companion object {
        fun fromName(value: String?): LifestyleGroup =
            values().firstOrNull { it.name == value } ?: OTHER
    }
}

data class Payee(
    val id: String,
    val vpa: String,
    val displayName: String,
    val parsedName: String?,
    val merchantCode: String?,
    val suggestedCategoryId: String?,
    val contactLookupKey: String? = null,
    val phoneNumber: String? = null,
    val isPerson: Boolean = false,
    val firstSeenAt: Long,
    val lastUsedAt: Long,
)

enum class PaymentStatus {
    PENDING,
    COMPLETED,
    FAILED,
    CANCELLED,
    EXPIRED,
}

enum class ExpenseSource {
    QR_SCAN,
    MANUAL,
    SAVED_PAYEE,
}

enum class PaymentMethod(val displayName: String) {
    UPI("UPI"),
    CASH("Cash"),
    CARD("Card"),
    OTHER("Other");

    companion object {
        fun fromName(value: String?): PaymentMethod =
            values().firstOrNull { it.name == value } ?: UPI
    }
}

data class Expense(
    val id: String,
    val amountMinor: Long,
    val currency: String,
    val category: Category,
    val payee: Payee?,
    val merchantName: String?,
    val note: String?,
    val status: PaymentStatus,
    val source: ExpenseSource,
    val paymentMethod: PaymentMethod,
    val intentSessionId: String?,
    val createdAt: Long,
    val completedAt: Long?,
)

data class Income(
    val id: String,
    val amountMinor: Long,
    val currency: String,
    val source: String,
    val description: String?,
    val note: String?,
    val createdAt: Long,
    val recordedAt: Long,
)

data class InstalledUpiApp(
    val packageName: String,
    val displayName: String,
    val isDefault: Boolean = false,
)
