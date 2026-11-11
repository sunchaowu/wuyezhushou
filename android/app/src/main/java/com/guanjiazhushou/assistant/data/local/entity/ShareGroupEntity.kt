package com.guanjiazhushou.assistant.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "share_groups",
    indices = [
        Index(value = ["status"]),
    ],
)
data class ShareGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "host_device_name")
    val hostDeviceName: String,
    @ColumnInfo(name = "verify_code")
    val verifyCode: String,
    @ColumnInfo(name = "status", defaultValue = "active")
    val status: String = "active",
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "dissolved_at")
    val dissolvedAt: Long? = null,
)
