package com.guanjiazhushou.assistant.domain.model

data class RoomWithFeatureIds(
    val room: Room,
    val featureTagIds: List<Long> = emptyList(),
)
