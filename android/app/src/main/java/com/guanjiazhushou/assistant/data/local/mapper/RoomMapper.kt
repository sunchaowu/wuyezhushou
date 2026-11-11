package com.guanjiazhushou.assistant.data.local.mapper

import com.guanjiazhushou.assistant.data.local.entity.RoomEntity
import com.guanjiazhushou.assistant.domain.model.PaymentType
import com.guanjiazhushou.assistant.domain.model.Room
import com.guanjiazhushou.assistant.domain.model.RoomType

fun RoomEntity.toDomain(): Room = Room(
    id = id,
    buildingId = buildingId,
    floor = floor,
    roomNumber = roomNumber,
    roomType = RoomType.fromKey(roomType) ?: RoomType.RESIDENTIAL,
    paymentType = PaymentType.fromKey(paymentType) ?: PaymentType.ARREARS,
    paymentMonths = paymentMonths,
    ownerFeedback = ownerFeedback,
    livingStatusKey = livingStatus,
    otherNotes = otherNotes,
    updatedAt = updatedAt,
)

fun Room.toEntity(): RoomEntity = RoomEntity(
    id = id,
    buildingId = buildingId,
    floor = floor,
    roomNumber = roomNumber,
    roomType = roomType.key,
    paymentType = paymentType.key,
    paymentMonths = paymentMonths,
    ownerFeedback = ownerFeedback,
    livingStatus = livingStatusKey,
    otherNotes = otherNotes,
    updatedAt = updatedAt,
)

fun List<RoomEntity>.toDomainRooms(): List<Room> = map { it.toDomain() }
