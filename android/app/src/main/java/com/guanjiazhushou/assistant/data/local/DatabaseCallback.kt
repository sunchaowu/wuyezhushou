package com.guanjiazhushou.assistant.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.guanjiazhushou.assistant.domain.model.AppThemeId
import com.guanjiazhushou.assistant.domain.model.LivingStatus
import com.guanjiazhushou.assistant.domain.model.LivingStatusColorDefaults

class DatabaseCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        BuiltinFeatureTagSeeder.seed(db)
        seedLivingStatusSettings(db)
        seedAppPreferences(db)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        BuiltinFeatureTagSeeder.ensurePresent(db)
    }

    private fun seedLivingStatusSettings(db: SupportSQLiteDatabase) {
        LivingStatus.entries.forEach { status ->
            val countsAsOccupied = when (status) {
                LivingStatus.SELF_LONG_TERM,
                LivingStatus.RENT_RENTED,
                -> 1
                else -> 0
            }
            db.execSQL(
                """
                INSERT INTO living_status_settings (
                    status_key, label, group_name, color_argb, counts_as_occupied, is_builtin, sort_order
                )
                VALUES (?, ?, '', ?, ?, 1, ?)
                """.trimIndent(),
                arrayOf<Any>(
                    status.key,
                    status.label,
                    LivingStatusColorDefaults.forKey(status.key),
                    countsAsOccupied,
                    status.ordinal,
                ),
            )
        }
    }

    private fun seedAppPreferences(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO app_preferences (key, value)
            VALUES ('theme_id', ?)
            """.trimIndent(),
            arrayOf(AppThemeId.BLUE_BUSINESS.key),
        )
    }
}
