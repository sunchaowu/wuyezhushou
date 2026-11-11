package com.guanjiazhushou.assistant.data.local.mapper

import com.guanjiazhushou.assistant.data.local.entity.FeatureTagEntity
import com.guanjiazhushou.assistant.domain.model.FeatureTag
import com.guanjiazhushou.assistant.domain.model.IconType

fun FeatureTagEntity.toDomain(): FeatureTag = FeatureTag(
    id = id,
    name = name,
    iconType = IconType.fromKey(iconType) ?: IconType.BUILTIN,
    iconKey = iconKey,
    iconPath = iconPath,
    isBuiltin = isBuiltin,
    sortOrder = sortOrder,
    createdAt = createdAt,
    description = description,
)

fun FeatureTag.toEntity(): FeatureTagEntity = FeatureTagEntity(
    id = id,
    name = name,
    iconType = iconType.key,
    iconKey = iconKey,
    iconPath = iconPath,
    isBuiltin = isBuiltin,
    sortOrder = sortOrder,
    createdAt = createdAt,
    description = description,
)

fun List<FeatureTagEntity>.toDomainFeatureTags(): List<FeatureTag> = map { it.toDomain() }
