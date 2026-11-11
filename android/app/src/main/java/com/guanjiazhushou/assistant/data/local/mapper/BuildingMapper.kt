package com.guanjiazhushou.assistant.data.local.mapper

import com.guanjiazhushou.assistant.data.local.entity.BuildingEntity
import com.guanjiazhushou.assistant.domain.model.Building
import com.guanjiazhushou.assistant.domain.model.BuildingKind
import com.guanjiazhushou.assistant.domain.model.SourceType

fun BuildingEntity.toDomain(): Building = Building(
    id = id,
    communityId = communityId,
    buildingKind = BuildingKind.fromKey(buildingKind) ?: BuildingKind.RESIDENTIAL,
    name = name,
    unitsPerFloor = unitsPerFloor,
    maxFloor = maxFloor,
    startFloor = startFloor,
    refugeFloors = refugeFloors,
    excludeRooms = excludeRooms,
    shopRooms = shopRooms,
    sourceType = SourceType.fromKey(sourceType) ?: SourceType.LOCAL,
    isReadonly = isReadonly,
    sourceDevice = sourceDevice,
    sourceGroupId = sourceGroupId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Building.toEntity(): BuildingEntity = BuildingEntity(
    id = id,
    communityId = communityId,
    buildingKind = buildingKind.key,
    name = name,
    unitsPerFloor = unitsPerFloor,
    maxFloor = maxFloor,
    startFloor = startFloor,
    refugeFloors = refugeFloors,
    excludeRooms = excludeRooms,
    shopRooms = shopRooms,
    sourceType = sourceType.key,
    isReadonly = isReadonly,
    sourceDevice = sourceDevice,
    sourceGroupId = sourceGroupId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun List<BuildingEntity>.toDomainBuildings(): List<Building> = map { it.toDomain() }
