package com.guanjiazhushou.assistant.domain.usecase

import com.guanjiazhushou.assistant.domain.exception.ReadOnlyBuildingException
import com.guanjiazhushou.assistant.domain.model.PaymentType
import com.guanjiazhushou.assistant.domain.repository.BuildingRepository
import com.guanjiazhushou.assistant.domain.repository.RoomRepository
import javax.inject.Inject

class BatchUpdateRoomPaymentUseCase @Inject constructor(
    private val roomRepository: RoomRepository,
    private val buildingRepository: BuildingRepository,
) {
    suspend operator fun invoke(
        buildingId: Long,
        roomIds: List<Long>,
        paymentType: PaymentType,
        paymentMonths: Int,
    ) {
        if (roomIds.isEmpty()) return
        val building = buildingRepository.getById(buildingId)
            ?: throw IllegalArgumentException("Building $buildingId not found")
        if (building.isReadonly) {
            throw ReadOnlyBuildingException(buildingId)
        }
        roomRepository.batchUpdatePaymentType(
            roomIds = roomIds,
            paymentType = paymentType,
            paymentMonths = paymentMonths.coerceAtLeast(1),
        )
    }
}
