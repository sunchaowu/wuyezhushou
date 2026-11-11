package com.guanjiazhushou.assistant.data.local.mapper

import com.guanjiazhushou.assistant.data.local.entity.ReminderEntity
import com.guanjiazhushou.assistant.domain.model.ReminderItem
import com.guanjiazhushou.assistant.domain.model.ReminderStatus
import com.guanjiazhushou.assistant.domain.model.ReminderTargetType

fun ReminderEntity.toDomain(): ReminderItem = ReminderItem(
    id = id,
    targetType = ReminderTargetType.fromKey(targetType),
    targetId = targetId,
    buildingId = buildingId,
    roomId = roomId,
    title = title,
    body = body,
    triggerAt = triggerAt,
    status = ReminderStatus.fromKey(status),
    createdAt = createdAt,
)

fun ReminderItem.toEntity(): ReminderEntity = ReminderEntity(
    id = id,
    targetType = targetType.key,
    targetId = targetId,
    buildingId = buildingId,
    roomId = roomId,
    title = title,
    body = body,
    triggerAt = triggerAt,
    status = status.key,
    createdAt = createdAt,
)
