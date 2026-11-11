package com.guanjiazhushou.assistant.domain.usecase

import com.guanjiazhushou.assistant.domain.repository.BuildingRepository
import javax.inject.Inject

class AddBottomShopUseCase @Inject constructor(
    private val buildingRepository: BuildingRepository,
) {
    suspend operator fun invoke(buildingId: Long, roomNumber: String): Long =
        buildingRepository.addBottomShopRoom(buildingId, roomNumber)
}
