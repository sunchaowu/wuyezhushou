package com.guanjiazhushou.assistant.data.local.mapper

import com.guanjiazhushou.assistant.data.local.entity.LivingStatusSettingEntity
import com.guanjiazhushou.assistant.domain.model.LivingStatus
import com.guanjiazhushou.assistant.domain.model.LivingStatusColorDefaults
import com.guanjiazhushou.assistant.domain.model.LivingStatusSetting

fun LivingStatusSettingEntity.toDomain(): LivingStatusSetting {
    val builtin = LivingStatus.fromKey(statusKey)
    return LivingStatusSetting(
        statusKey = statusKey,
        label = label.ifEmpty { builtin?.label.orEmpty() }.ifEmpty { statusKey },
        colorArgb = colorArgb.takeIf { it != 0L } ?: LivingStatusColorDefaults.forKey(statusKey),
        countsAsOccupied = countsAsOccupied,
        isBuiltin = isBuiltin,
        sortOrder = sortOrder,
    )
}

fun LivingStatusSetting.toEntity(): LivingStatusSettingEntity = LivingStatusSettingEntity(
    statusKey = statusKey,
    label = label,
    groupName = "",
    colorArgb = colorArgb,
    countsAsOccupied = countsAsOccupied,
    isBuiltin = isBuiltin,
    sortOrder = sortOrder,
)

fun List<LivingStatusSettingEntity>.toDomainLivingStatusSettings(): List<LivingStatusSetting> =
    map { it.toDomain() }
