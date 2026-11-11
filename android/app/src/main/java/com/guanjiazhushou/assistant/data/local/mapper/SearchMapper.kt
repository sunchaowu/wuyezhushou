package com.guanjiazhushou.assistant.data.local.mapper

import com.guanjiazhushou.assistant.data.local.entity.SearchMatchRow
import com.guanjiazhushou.assistant.domain.model.SearchMatch
import com.guanjiazhushou.assistant.domain.model.SearchMatchType

fun SearchMatchRow.toDomain(): SearchMatch? {
    val type = when (matchType) {
        "building" -> SearchMatchType.BUILDING
        "room" -> SearchMatchType.ROOM
        "owner_feedback" -> SearchMatchType.OWNER_FEEDBACK
        "notes" -> SearchMatchType.NOTES
        "communication" -> SearchMatchType.COMMUNICATION
        else -> return null
    }
    return SearchMatch(
        matchType = type,
        buildingId = buildingId,
        buildingName = buildingName,
        roomId = roomId,
        roomNumber = roomNumber,
        snippet = snippet,
    )
}

fun List<SearchMatchRow>.toDomainMatches(): List<SearchMatch> =
    mapNotNull { it.toDomain() }
