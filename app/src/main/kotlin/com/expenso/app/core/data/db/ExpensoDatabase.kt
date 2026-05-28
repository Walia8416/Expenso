package com.expenso.app.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.expenso.app.core.data.db.dao.CategoryDao
import com.expenso.app.core.data.db.dao.ExpenseDao
import com.expenso.app.core.data.db.dao.IncomeDao
import com.expenso.app.core.data.db.dao.PayeeDao
import com.expenso.app.core.data.db.dao.PaymentIntentDao
import com.expenso.app.core.data.db.entities.CategoryEntity
import com.expenso.app.core.data.db.entities.ExpenseEntity
import com.expenso.app.core.data.db.entities.IncomeEntity
import com.expenso.app.core.data.db.entities.PayeeEntity
import com.expenso.app.core.data.db.entities.PaymentIntentEntity

@Database(
    entities = [
        ExpenseEntity::class,
        CategoryEntity::class,
        PayeeEntity::class,
        PaymentIntentEntity::class,
        IncomeEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class ExpensoDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun payeeDao(): PayeeDao
    abstract fun paymentIntentDao(): PaymentIntentDao
    abstract fun incomeDao(): IncomeDao

    companion object {
        const val DB_NAME = "expenso.db"
    }
}
