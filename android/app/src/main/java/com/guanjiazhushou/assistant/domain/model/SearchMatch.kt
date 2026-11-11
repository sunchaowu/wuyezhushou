package com.guanjiazhushou.assistant.domain.model

data class SearchMatch(
    val matchType: SearchMatchType,
    val buildingId: Long,
    val buildingName: String,
    val roomId: Long?,
    val roomNumber: String?,
    val snippet: String,
)
