package com.guanjiazhushou.assistant.domain.model

data class BuildingMetrics(
    val totalRooms: Int = 0,
    val occupancyRate: Float = 0f,
    val collectionRate: Float = 0f,
    val rentRate: Float = 0f,
    val arrearsCount: Int = 0,
)
