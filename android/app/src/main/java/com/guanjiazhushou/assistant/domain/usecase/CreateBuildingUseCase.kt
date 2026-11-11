package com.guanjiazhushou.assistant.domain.usecase

import com.guanjiazhushou.assistant.domain.model.Building
import com.guanjiazhushou.assistant.domain.model.BuildingKind
import com.guanjiazhushou.assistant.domain.model.PaymentType
import com.guanjiazhushou.assistant.domain.model.SourceType
import com.guanjiazhushou.assistant.domain.repository.BuildingRepository
import javax.inject.Inject

data class CreateBuildingParams(
    val communityId: Long,
    val buildingKind: BuildingKind = BuildingKind.RESIDENTIAL,
    val name: String,
    val unitsPerFloor: Int,
    val maxFloor: Int,
    val startFloor: Int,
    val refugeFloors: String = "",
    val excludeRooms: String = "",
    val shopRooms: String = "",
    val defaultPaymentType: PaymentType = PaymentType.ARREARS,
)

class CreateBuildingUseCase @Inject constructor(
    private val buildingRepository: BuildingRepository,
    private val generateRoomsUseCase: GenerateRoomsUseCase,
) {
    suspend operator fun invoke(params: CreateBuildingParams): Long {
        val now = System.currentTimeMillis()
        val building = Building(
            communityId = params.communityId,
            buildingKind = params.buildingKind,
            name = params.name,
            unitsPerFloor = params.unitsPerFloor,
            maxFloor = params.maxFloor,
            startFloor = params.startFloor,
            refugeFloors = params.refugeFloors,
            excludeRooms = params.excludeRooms,
            shopRooms = params.shopRooms,
            sourceType = SourceType.LOCAL,
            isReadonly = false,
            createdAt = now,
            updatedAt = now,
        )
        val rooms = generateRoomsUseCase(
            buildingId = 0,
            buildingKind = params.buildingKind,
            startFloor = params.startFloor,
            maxFloor = params.maxFloor,
            unitsPerFloor = params.unitsPerFloor,
            refugeFloors = params.refugeFloors,
            excludeRooms = params.excludeRooms,
            shopRooms = params.shopRooms,
            defaultPaymentType = params.defaultPaymentType,
            timestamp = now,
        )
        return buildingRepository.createBuildingWithRooms(building, rooms)
    }
}
