package com.expenso.app.di

import android.content.Context
import androidx.room.Room
import com.expenso.app.core.data.db.DatabaseKeyProvider
import com.expenso.app.core.data.db.ExpensoDatabase
import com.expenso.app.core.data.db.SeedCallback
import com.expenso.app.core.data.db.dao.CategoryDao
import com.expenso.app.core.data.db.dao.ExpenseDao
import com.expenso.app.core.data.db.dao.IncomeDao
import com.expenso.app.core.data.db.dao.PayeeDao
import com.expenso.app.core.data.db.dao.PaymentIntentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keyProvider: DatabaseKeyProvider,
    ): ExpensoDatabase {
        System.loadLibrary("sqlcipher")
        val passphrase = keyProvider.getOrCreatePassphrase()
        val factory = SupportOpenHelperFactory(passphrase)
        return Room.databaseBuilder(
            context.applicationContext,
            ExpensoDatabase::class.java,
            ExpensoDatabase.DB_NAME,
        )
            .openHelperFactory(factory)
            .addCallback(SeedCallback())
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideExpenseDao(db: ExpensoDatabase): ExpenseDao = db.expenseDao()
    @Provides fun provideCategoryDao(db: ExpensoDatabase): CategoryDao = db.categoryDao()
    @Provides fun providePayeeDao(db: ExpensoDatabase): PayeeDao = db.payeeDao()
    @Provides fun providePaymentIntentDao(db: ExpensoDatabase): PaymentIntentDao = db.paymentIntentDao()
    @Provides fun provideIncomeDao(db: ExpensoDatabase): IncomeDao = db.incomeDao()
}
