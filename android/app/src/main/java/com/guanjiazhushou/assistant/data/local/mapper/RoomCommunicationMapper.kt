package com.guanjiazhushou.assistant.data.local.mapper

import com.guanjiazhushou.assistant.data.local.entity.RoomCommunicationEntity
import com.guanjiazhushou.assistant.domain.model.CommunicationRecordType
import com.guanjiazhushou.assistant.domain.model.RoomCommunicationRecord

fun RoomCommunicationEntity.toDomain(): RoomCommunicationRecord = RoomCommunicationRecord(
    id = id,
    roomId = roomId,
    type = CommunicationRecordType.fromKey(recordType) ?: CommunicationRecordType.TEXT,
    content = content,
    audioPath = audioPath,
    durationMs = durationMs,
    createdAt = createdAt,
)

fun List<RoomCommunicationEntity>.toDomainRecords(): List<RoomCommunicationRecord> =
    map { it.toDomain() }

fun RoomCommunicationRecord.toEntity(): RoomCommunicationEntity = RoomCommunicationEntity(
    id = id,
    roomId = roomId,
    recordType = type.key,
    content = content,
    audioPath = audioPath,
    durationMs = durationMs,
    createdAt = createdAt,
)
