package com.guanjiazhushou.assistant.domain.model

data class Building(
    val id: Long = 0,
    val communityId: Long,
    val buildingKind: BuildingKind = BuildingKind.RESIDENTIAL,
    val name: String,
    val unitsPerFloor: Int,
    val maxFloor: Int,
    val startFloor: Int,
    val refugeFloors: String = "",
    val excludeRooms: String = "",
    val shopRooms: String = "",
    val sourceType: SourceType = SourceType.LOCAL,
    val isReadonly: Boolean = false,
    val sourceDevice: String? = null,
    val sourceGroupId: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
