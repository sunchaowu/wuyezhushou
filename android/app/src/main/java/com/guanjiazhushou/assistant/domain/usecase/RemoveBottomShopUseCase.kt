package com.guanjiazhushou.assistant.domain.usecase

import com.guanjiazhushou.assistant.domain.repository.BuildingRepository
import javax.inject.Inject

class RemoveBottomShopUseCase @Inject constructor(
    private val buildingRepository: BuildingRepository,
) {
    suspend operator fun invoke(buildingId: Long, roomId: Long) {
        buildingRepository.removeBottomShopRoom(buildingId, roomId)
    }
}
