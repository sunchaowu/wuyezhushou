package com.guanjiazhushou.assistant.domain.usecase

import com.guanjiazhushou.assistant.domain.model.BuildingKind
import com.guanjiazhushou.assistant.domain.model.PaymentType
import com.guanjiazhushou.assistant.domain.model.Room
import com.guanjiazhushou.assistant.domain.util.RoomGenerator
import javax.inject.Inject

class GenerateRoomsUseCase @Inject constructor() {
    operator fun invoke(
        buildingId: Long,
        buildingKind: BuildingKind = BuildingKind.RESIDENTIAL,
        startFloor: Int,
        maxFloor: Int,
        unitsPerFloor: Int,
        refugeFloors: String,
        excludeRooms: String,
        shopRooms: String,
        defaultPaymentType: PaymentType = PaymentType.ARREARS,
        timestamp: Long = System.currentTimeMillis(),
    ): List<Room> = RoomGenerator.generateRooms(
        buildingId = buildingId,
        buildingKind = buildingKind,
        startFloor = startFloor,
        maxFloor = maxFloor,
        unitsPerFloor = unitsPerFloor,
        refugeFloors = refugeFloors,
        excludeRooms = excludeRooms,
        shopRooms = shopRooms,
        defaultPaymentType = defaultPaymentType,
        timestamp = timestamp,
    )
}
