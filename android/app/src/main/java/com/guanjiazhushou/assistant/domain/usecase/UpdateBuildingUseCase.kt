package com.guanjiazhushou.assistant.domain.usecase

import com.guanjiazhushou.assistant.domain.exception.ReadOnlyBuildingException
import com.guanjiazhushou.assistant.domain.model.Building
import com.guanjiazhushou.assistant.domain.model.PaymentType
import com.guanjiazhushou.assistant.domain.repository.BuildingRepository
import javax.inject.Inject

class UpdateBuildingUseCase @Inject constructor(
    private val buildingRepository: BuildingRepository,
) {
    suspend operator fun invoke(
        building: Building,
        defaultPaymentType: PaymentType,
    ) {
        if (building.isReadonly) {
            throw ReadOnlyBuildingException(building.id)
        }
        buildingRepository.updateBuildingWithRoomSync(building, defaultPaymentType)
    }
}
