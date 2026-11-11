package com.guanjiazhushou.assistant.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "feature_tags",
    indices = [
        Index(value = ["name"], unique = true),
    ],
)
data class FeatureTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "icon_type")
    val iconType: String,
    @ColumnInfo(name = "icon_key")
    val iconKey: String? = null,
    @ColumnInfo(name = "icon_path")
    val iconPath: String? = null,
    @ColumnInfo(name = "is_builtin", defaultValue = "0")
    val isBuiltin: Boolean = false,
    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "description")
    val description: String? = null,
)
