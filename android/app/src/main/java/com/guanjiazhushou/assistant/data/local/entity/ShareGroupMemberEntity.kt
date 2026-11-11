package com.guanjiazhushou.assistant.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "share_group_members",
    primaryKeys = ["group_id", "member_device_name"],
    foreignKeys = [
        ForeignKey(
            entity = ShareGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["group_id"]),
    ],
)
data class ShareGroupMemberEntity(
    @ColumnInfo(name = "group_id")
    val groupId: Long,
    @ColumnInfo(name = "member_device_name")
    val memberDeviceName: String,
    @ColumnInfo(name = "joined_at")
    val joinedAt: Long,
    @ColumnInfo(name = "left_at")
    val leftAt: Long? = null,
)
