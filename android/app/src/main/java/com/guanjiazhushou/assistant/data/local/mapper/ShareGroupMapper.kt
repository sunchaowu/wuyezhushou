package com.guanjiazhushou.assistant.data.local.mapper

import com.guanjiazhushou.assistant.data.local.entity.ShareGroupBuildingEntity
import com.guanjiazhushou.assistant.data.local.entity.ShareGroupEntity
import com.guanjiazhushou.assistant.data.local.entity.ShareGroupMemberEntity
import com.guanjiazhushou.assistant.domain.model.ShareGroup
import com.guanjiazhushou.assistant.domain.model.ShareGroupBuilding
import com.guanjiazhushou.assistant.domain.model.ShareGroupMember

fun ShareGroupEntity.toDomain(): ShareGroup = ShareGroup(
    id = id,
    name = name,
    hostDeviceName = hostDeviceName,
    verifyCode = verifyCode,
    status = status,
    createdAt = createdAt,
    dissolvedAt = dissolvedAt,
)

fun ShareGroup.toEntity(): ShareGroupEntity = ShareGroupEntity(
    id = id,
    name = name,
    hostDeviceName = hostDeviceName,
    verifyCode = verifyCode,
    status = status,
    createdAt = createdAt,
    dissolvedAt = dissolvedAt,
)

fun ShareGroupMemberEntity.toDomain(): ShareGroupMember = ShareGroupMember(
    groupId = groupId,
    memberDeviceName = memberDeviceName,
    joinedAt = joinedAt,
    leftAt = leftAt,
)

fun ShareGroupMember.toEntity(): ShareGroupMemberEntity = ShareGroupMemberEntity(
    groupId = groupId,
    memberDeviceName = memberDeviceName,
    joinedAt = joinedAt,
    leftAt = leftAt,
)

fun ShareGroupBuildingEntity.toDomain(): ShareGroupBuilding = ShareGroupBuilding(
    groupId = groupId,
    buildingId = buildingId,
    sharedAt = sharedAt,
)

fun ShareGroupBuilding.toEntity(): ShareGroupBuildingEntity = ShareGroupBuildingEntity(
    groupId = groupId,
    buildingId = buildingId,
    sharedAt = sharedAt,
)

fun List<ShareGroupEntity>.toDomainShareGroups(): List<ShareGroup> = map { it.toDomain() }
