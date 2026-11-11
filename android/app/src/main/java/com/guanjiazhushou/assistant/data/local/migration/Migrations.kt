package com.guanjiazhushou.assistant.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.guanjiazhushou.assistant.domain.model.LivingStatus
import com.guanjiazhushou.assistant.domain.model.LivingStatusColorDefaults

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE living_status_settings
            ADD COLUMN label TEXT NOT NULL DEFAULT ''
            """.trimIndent(),
        )
        db.execSQL(
            """
            ALTER TABLE living_status_settings
            ADD COLUMN group_name TEXT NOT NULL DEFAULT ''
            """.trimIndent(),
        )
        db.execSQL(
            """
            ALTER TABLE living_status_settings
            ADD COLUMN is_builtin INTEGER NOT NULL DEFAULT 1
            """.trimIndent(),
        )
        db.execSQL(
            """
            ALTER TABLE living_status_settings
            ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0
            """.trimIndent(),
        )
        LivingStatus.entries.forEachIndexed { index, status ->
            db.execSQL(
                """
                UPDATE living_status_settings
                SET label = ?, group_name = ?, is_builtin = 1, sort_order = ?
                WHERE status_key = ?
                """.trimIndent(),
                arrayOf<Any>(status.label, status.group, index, status.key),
            )
        }
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE living_status_settings
            ADD COLUMN color_argb INTEGER NOT NULL DEFAULT 0
            """.trimIndent(),
        )
        LivingStatus.entries.forEach { status ->
            db.execSQL(
                """
                UPDATE living_status_settings
                SET color_argb = ?
                WHERE status_key = ?
                """.trimIndent(),
                arrayOf<Any>(
                    LivingStatusColorDefaults.forKey(status.key),
                    status.key,
                ),
            )
        }
        db.execSQL(
            """
            UPDATE living_status_settings
            SET color_argb = ?
            WHERE color_argb = 0
            """.trimIndent(),
            arrayOf<Any>(LivingStatusColorDefaults.presetOptions.first()),
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE buildings
            ADD COLUMN building_kind TEXT NOT NULL DEFAULT 'residential'
            """.trimIndent(),
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS room_communications (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                room_id INTEGER NOT NULL,
                record_type TEXT NOT NULL,
                content TEXT NOT NULL DEFAULT '',
                audio_path TEXT,
                duration_ms INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(room_id) REFERENCES rooms(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_room_communications_room_id
            ON room_communications (room_id)
            """.trimIndent(),
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE feature_tags
            ADD COLUMN description TEXT
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS room_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                building_id INTEGER NOT NULL,
                room_id INTEGER NOT NULL,
                type TEXT NOT NULL,
                summary TEXT NOT NULL,
                raw_text TEXT,
                status TEXT NOT NULL,
                source TEXT NOT NULL,
                reminder_at INTEGER,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                completed_at INTEGER,
                FOREIGN KEY(room_id) REFERENCES rooms(id) ON DELETE CASCADE,
                FOREIGN KEY(building_id) REFERENCES buildings(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_room_events_building_id ON room_events (building_id)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_room_events_room_id ON room_events (room_id)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_room_events_status ON room_events (status)",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS reminders (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                target_type TEXT NOT NULL,
                target_id INTEGER NOT NULL,
                building_id INTEGER NOT NULL,
                room_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                trigger_at INTEGER NOT NULL,
                status TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_reminders_status ON reminders (status)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_reminders_trigger_at ON reminders (trigger_at)",
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_reminders_target_type_target_id
            ON reminders (target_type, target_id)
            """.trimIndent(),
        )
    }
}
