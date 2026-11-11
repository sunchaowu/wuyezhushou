package com.guanjiazhushou.assistant.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "room_communications",
    foreignKeys = [
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["room_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["room_id"])],
)
data class RoomCommunicationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "room_id")
    val roomId: Long,
    @ColumnInfo(name = "record_type")
    val recordType: String,
    @ColumnInfo(name = "content", defaultValue = "")
    val content: String = "",
    @ColumnInfo(name = "audio_path")
    val audioPath: String? = null,
    @ColumnInfo(name = "duration_ms", defaultValue = "0")
    val durationMs: Long = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
