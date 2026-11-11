package com.guanjiazhushou.assistant.domain.model

data class Room(
    val id: Long = 0,
    val buildingId: Long,
    val floor: Int,
    val roomNumber: String,
    val roomType: RoomType = RoomType.RESIDENTIAL,
    val paymentType: PaymentType = PaymentType.ARREARS,
    val paymentMonths: Int = 1,
    val ownerFeedback: String = "",
    val livingStatusKey: String = LivingStatus.VACANT_UNDELIVERED.key,
    val otherNotes: String = "",
    val updatedAt: Long,
)
