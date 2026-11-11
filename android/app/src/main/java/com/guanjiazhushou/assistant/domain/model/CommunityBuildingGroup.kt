package com.guanjiazhushou.assistant.domain.model

data class CommunityBuildingGroup(
    val communityId: Long,
    val communityName: String,
    val buildings: List<Building>,
)
