package com.guanjiazhushou.assistant.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import com.guanjiazhushou.assistant.domain.model.BuiltinFeatureTags

object BuiltinFeatureTagSeeder {
    fun seed(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        BuiltinFeatureTags.definitions.forEach { definition ->
            db.execSQL(
                """
                INSERT INTO feature_tags (name, icon_type, icon_key, icon_path, is_builtin, sort_order, created_at)
                VALUES (?, 'builtin', ?, NULL, 1, ?, ?)
                """.trimIndent(),
                arrayOf<Any>(definition.name, definition.iconKey, definition.sortOrder, now),
            )
        }
    }

    fun ensurePresent(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        BuiltinFeatureTags.definitions.forEach { definition ->
            val cursor = db.query(
                """
                SELECT id FROM feature_tags
                WHERE is_builtin = 1 AND (icon_key = ? OR sort_order = ?)
                LIMIT 1
                """.trimIndent(),
                arrayOf<Any>(definition.iconKey, definition.sortOrder),
            )
            val exists = cursor.moveToFirst()
            cursor.close()
            if (!exists) {
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO feature_tags (name, icon_type, icon_key, icon_path, is_builtin, sort_order, created_at)
                    VALUES (?, 'builtin', ?, NULL, 1, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any>(definition.name, definition.iconKey, definition.sortOrder, now),
                )
            } else {
                db.execSQL(
                    """
                    UPDATE feature_tags
                    SET icon_type = 'builtin',
                        icon_key = ?,
                        icon_path = NULL,
                        is_builtin = 1
                    WHERE is_builtin = 1 AND (icon_key = ? OR sort_order = ?)
                    """.trimIndent(),
                    arrayOf<Any>(definition.iconKey, definition.iconKey, definition.sortOrder),
                )
            }
        }
    }
}
