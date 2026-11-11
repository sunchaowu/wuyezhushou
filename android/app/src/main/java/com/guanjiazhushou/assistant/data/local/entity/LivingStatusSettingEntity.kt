package com.guanjiazhushou.assistant.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "living_status_settings")
data class LivingStatusSettingEntity(
    @PrimaryKey
    @ColumnInfo(name = "status_key")
    val statusKey: String,
    @ColumnInfo(name = "label")
    val label: String,
    @ColumnInfo(name = "group_name")
    val groupName: String = "",
    @ColumnInfo(name = "color_argb")
    val colorArgb: Long,
    @ColumnInfo(name = "counts_as_occupied", defaultValue = "0")
    val countsAsOccupied: Boolean = false,
    @ColumnInfo(name = "is_builtin", defaultValue = "1")
    val isBuiltin: Boolean = true,
    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Int = 0,
)
