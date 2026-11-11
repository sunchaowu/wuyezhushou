package com.guanjiazhushou.assistant.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "share_group_buildings",
    primaryKeys = ["group_id", "building_id"],
    foreignKeys = [
        ForeignKey(
            entity = ShareGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
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
    ],
)
data class ShareGroupBuildingEntity(
    @ColumnInfo(name = "group_id")
    val groupId: Long,
    @ColumnInfo(name = "building_id")
    val buildingId: Long,
    @ColumnInfo(name = "shared_at")
    val sharedAt: Long,
)
