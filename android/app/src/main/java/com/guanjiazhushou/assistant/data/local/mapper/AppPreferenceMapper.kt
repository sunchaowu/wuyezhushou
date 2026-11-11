package com.guanjiazhushou.assistant.data.local.mapper

import com.guanjiazhushou.assistant.data.local.entity.AppPreferenceEntity
import com.guanjiazhushou.assistant.domain.model.AppPreference

fun AppPreferenceEntity.toDomain(): AppPreference = AppPreference(
    key = key,
    value = value,
)

fun AppPreference.toEntity(): AppPreferenceEntity = AppPreferenceEntity(
    key = key,
    value = value,
)
