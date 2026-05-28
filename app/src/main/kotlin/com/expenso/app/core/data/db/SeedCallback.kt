package com.expenso.app.core.data.db

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Seeds the `category` table with defaults on first DB create.
 */
class SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        for (c in SeedCategories.defaults) {
            db.execSQL(
                """
                INSERT OR IGNORE INTO category
                    (id, name, emoji, colorHex, sortOrder, isArchived, isDefault)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf(
                    c.id,
                    c.name,
                    c.emoji,
                    c.colorHex,
                    c.sortOrder,
                    if (c.isArchived) 1 else 0,
                    if (c.isDefault) 1 else 0,
                ),
            )
        }
    }
}
