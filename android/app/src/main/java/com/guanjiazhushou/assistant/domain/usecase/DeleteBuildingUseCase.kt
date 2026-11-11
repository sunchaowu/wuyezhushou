package com.guanjiazhushou.assistant.domain.usecase

import com.guanjiazhushou.assistant.domain.exception.ReadOnlyBuildingException
import com.guanjiazhushou.assistant.domain.repository.BuildingRepository
import javax.inject.Inject

class DeleteBuildingUseCase @Inject constructor(
    private val buildingRepository: BuildingRepository,
) {
    suspend operator fun invoke(buildingId: Long) {
        val building = buildingRepository.getById(buildingId)
            ?: throw IllegalStateException("楼栋不存在")
        if (building.isReadonly) {
            throw ReadOnlyBuildingException(buildingId)
        }
        buildingRepository.deleteBuilding(buildingId)
    }
}
