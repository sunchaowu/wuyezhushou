package com.guanjiazhushou.assistant.domain.usecase

import com.guanjiazhushou.assistant.domain.exception.ReadOnlyBuildingException
import com.guanjiazhushou.assistant.domain.model.BatchRoomUpdateParams
import com.guanjiazhushou.assistant.domain.repository.BuildingRepository
import com.guanjiazhushou.assistant.domain.repository.RoomRepository
import javax.inject.Inject

class BatchUpdateRoomsUseCase @Inject constructor(
    private val roomRepository: RoomRepository,
    private val buildingRepository: BuildingRepository,
) {
    suspend operator fun invoke(
        buildingId: Long,
        roomIds: List<Long>,
        params: BatchRoomUpdateParams,
    ) {
        if (roomIds.isEmpty() || !params.isValid) return
        val building = buildingRepository.getById(buildingId)
            ?: throw IllegalArgumentException("Building $buildingId not found")
        if (building.isReadonly) {
            throw ReadOnlyBuildingException(buildingId)
        }
        if (params.updatePayment) {
            val paymentType = checkNotNull(params.paymentType)
            roomRepository.batchUpdatePaymentType(
                roomIds = roomIds,
                paymentType = paymentType,
                paymentMonths = params.paymentMonths?.coerceAtLeast(1) ?: 1,
            )
        }
        if (params.updateLivingStatus) {
            roomRepository.batchUpdateLivingStatus(
                roomIds = roomIds,
                livingStatusKey = checkNotNull(params.livingStatusKey),
            )
        }
        if (params.updateFeatureTags) {
            roomRepository.batchUpdateFeatureTags(
                roomIds = roomIds,
                mode = params.featureTagMode,
                tagIds = params.featureTagIds,
            )
        }
    }
}
