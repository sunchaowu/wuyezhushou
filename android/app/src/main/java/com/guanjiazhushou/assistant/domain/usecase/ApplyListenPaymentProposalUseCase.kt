package com.guanjiazhushou.assistant.domain.usecase

import com.guanjiazhushou.assistant.domain.model.PaymentType
import com.guanjiazhushou.assistant.domain.model.Room
import com.guanjiazhushou.assistant.domain.repository.RoomRepository
import javax.inject.Inject

class ApplyListenPaymentProposalUseCase @Inject constructor(
    private val roomRepository: RoomRepository,
    private val updateRoomUseCase: UpdateRoomUseCase,
) {
    suspend operator fun invoke(
        roomId: Long,
        paymentType: PaymentType,
        paymentMonths: Int,
        featureTagIds: List<Long>? = null,
    ) {
        val existing = roomRepository.getById(roomId)
            ?: error("房间不存在: $roomId")
        val room: Room = existing.copy(
            paymentType = paymentType,
            paymentMonths = paymentMonths.coerceAtLeast(0),
            updatedAt = System.currentTimeMillis(),
        )
        val tags = featureTagIds ?: roomRepository.getFeatureTagIdsForRoom(roomId)
        updateRoomUseCase(room, tags)
    }
}
