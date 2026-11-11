package com.guanjiazhushou.assistant.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rooms",
    foreignKeys = [
        ForeignKey(
            entity = BuildingEntity::class,
            parentColumns = ["id"],
            childColumns = ["building_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["building_id", "room_number"], unique = true),
        Index(value = ["building_id", "floor"]),
        Index(value = ["living_status"]),
        Index(value = ["payment_type"]),
    ],
)
data class RoomEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "building_id")
    val buildingId: Long,
    @ColumnInfo(name = "floor")
    val floor: Int,
    @ColumnInfo(name = "room_number")
    val roomNumber: String,
    @ColumnInfo(name = "room_type", defaultValue = "residential")
    val roomType: String = "residential",
    @ColumnInfo(name = "payment_type", defaultValue = "arrears")
    val paymentType: String = "arrears",
    @ColumnInfo(name = "payment_months", defaultValue = "1")
    val paymentMonths: Int = 1,
    @ColumnInfo(name = "owner_feedback", defaultValue = "")
    val ownerFeedback: String = "",
    @ColumnInfo(name = "living_status", defaultValue = "vacant_undelivered")
    val livingStatus: String = "vacant_undelivered",
    @ColumnInfo(name = "other_notes", defaultValue = "")
    val otherNotes: String = "",
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
