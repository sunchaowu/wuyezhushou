package com.guanjiazhushou.assistant.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["status"]),
        Index(value = ["trigger_at"]),
        Index(value = ["target_type", "target_id"]),
    ],
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "target_type")
    val targetType: String,
    @ColumnInfo(name = "target_id")
    val targetId: Long,
    @ColumnInfo(name = "building_id")
    val buildingId: Long,
    @ColumnInfo(name = "room_id")
    val roomId: Long,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "body")
    val body: String,
    @ColumnInfo(name = "trigger_at")
    val triggerAt: Long,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
