package com.guanjiazhushou.assistant.data.local.mapper

import com.guanjiazhushou.assistant.data.local.entity.CommunityEntity
import com.guanjiazhushou.assistant.domain.model.Community

fun CommunityEntity.toDomain(): Community = Community(
    id = id,
    name = name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Community.toEntity(): CommunityEntity = CommunityEntity(
    id = id,
    name = name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
