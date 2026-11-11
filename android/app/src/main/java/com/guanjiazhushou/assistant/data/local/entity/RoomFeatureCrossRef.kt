package com.guanjiazhushou.assistant.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "room_features",
    primaryKeys = ["room_id", "feature_tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["room_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FeatureTagEntity::class,
            parentColumns = ["id"],
            childColumns = ["feature_tag_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["feature_tag_id"]),
    ],
)
data class RoomFeatureCrossRef(
    @ColumnInfo(name = "room_id")
    val roomId: Long,
    @ColumnInfo(name = "feature_tag_id")
    val featureTagId: Long,
)
