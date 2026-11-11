package com.guanjiazhushou.assistant.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "buildings",
    foreignKeys = [
        ForeignKey(
            entity = CommunityEntity::class,
            parentColumns = ["id"],
            childColumns = ["community_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ShareGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_group_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["community_id"]),
        Index(value = ["source_group_id"]),
        Index(value = ["source_type"]),
    ],
)
data class BuildingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "community_id")
    val communityId: Long,
    @ColumnInfo(name = "building_kind", defaultValue = "residential")
    val buildingKind: String = "residential",
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "units_per_floor")
    val unitsPerFloor: Int,
    @ColumnInfo(name = "max_floor")
    val maxFloor: Int,
    @ColumnInfo(name = "start_floor")
    val startFloor: Int,
    @ColumnInfo(name = "refuge_floors", defaultValue = "")
    val refugeFloors: String = "",
    @ColumnInfo(name = "exclude_rooms", defaultValue = "")
    val excludeRooms: String = "",
    @ColumnInfo(name = "shop_rooms", defaultValue = "")
    val shopRooms: String = "",
    @ColumnInfo(name = "source_type", defaultValue = "local")
    val sourceType: String = "local",
    @ColumnInfo(name = "is_readonly", defaultValue = "0")
    val isReadonly: Boolean = false,
    @ColumnInfo(name = "source_device")
    val sourceDevice: String? = null,
    @ColumnInfo(name = "source_group_id")
    val sourceGroupId: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
