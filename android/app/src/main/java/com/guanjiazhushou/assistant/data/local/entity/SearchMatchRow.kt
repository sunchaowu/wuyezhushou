package com.guanjiazhushou.assistant.data.local.entity

import androidx.room.ColumnInfo

data class SearchMatchRow(
    @ColumnInfo(name = "match_type")
    val matchType: String,
    @ColumnInfo(name = "building_id")
    val buildingId: Long,
    @ColumnInfo(name = "building_name")
    val buildingName: String,
    @ColumnInfo(name = "room_id")
    val roomId: Long?,
    @ColumnInfo(name = "room_number")
    val roomNumber: String?,
    @ColumnInfo(name = "snippet")
    val snippet: String,
)
