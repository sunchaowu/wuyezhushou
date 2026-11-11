package com.guanjiazhushou.assistant.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "room_events",
    foreignKeys = [
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["room_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = BuildingEntity::class,
            parentColumns = ["id"],
            childColumns = ["building_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["building_id"]),
        Index(value = ["room_id"]),
        Index(value = ["status"]),
    ],
)
data class RoomEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "building_id")
    val buildingId: Long,
    @ColumnInfo(name = "room_id")
    val roomId: Long,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "summary")
    val summary: String,
    @ColumnInfo(name = "raw_text")
    val rawText: String? = null,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "source")
    val source: String,
    @ColumnInfo(name = "reminder_at")
    val reminderAt: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,
)
