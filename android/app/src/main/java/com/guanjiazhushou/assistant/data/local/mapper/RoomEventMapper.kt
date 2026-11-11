package com.guanjiazhushou.assistant.data.local.mapper

import com.guanjiazhushou.assistant.data.local.entity.RoomEventEntity
import com.guanjiazhushou.assistant.domain.model.RoomEvent
import com.guanjiazhushou.assistant.domain.model.RoomEventSource
import com.guanjiazhushou.assistant.domain.model.RoomEventStatus
import com.guanjiazhushou.assistant.domain.model.RoomEventType

fun RoomEventEntity.toDomain(): RoomEvent = RoomEvent(
    id = id,
    buildingId = buildingId,
    roomId = roomId,
    type = RoomEventType.fromKey(type),
    summary = summary,
    rawText = rawText,
    status = RoomEventStatus.fromKey(status),
    source = RoomEventSource.fromKey(source),
    reminderAt = reminderAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt,
)

fun RoomEvent.toEntity(): RoomEventEntity = RoomEventEntity(
    id = id,
    buildingId = buildingId,
    roomId = roomId,
    type = type.key,
    summary = summary,
    rawText = rawText,
    status = status.key,
    source = source.key,
    reminderAt = reminderAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    completedAt = completedAt,
)
