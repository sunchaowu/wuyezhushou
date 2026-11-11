package com.guanjiazhushou.assistant.domain.usecase

import com.guanjiazhushou.assistant.domain.exception.ReadOnlyBuildingException
import com.guanjiazhushou.assistant.domain.model.BuildingKind
import com.guanjiazhushou.assistant.domain.model.Room
import com.guanjiazhushou.assistant.domain.repository.BuildingRepository
import com.guanjiazhushou.assistant.domain.repository.RoomRepository
import com.guanjiazhushou.assistant.domain.model.RoomType
import com.guanjiazhushou.assistant.domain.util.RoomGenerator
import com.guanjiazhushou.assistant.domain.util.ShopRoomsFormatter
import javax.inject.Inject

class UpdateRoomUseCase @Inject constructor(
    private val roomRepository: RoomRepository,
    private val buildingRepository: BuildingRepository,
) {
    suspend operator fun invoke(room: Room, featureTagIds: List<Long> = emptyList()) {
        val building = buildingRepository.getById(room.buildingId)
            ?: throw IllegalArgumentException("Building ${room.buildingId} not found")
        if (building.isReadonly) {
            throw ReadOnlyBuildingException(room.buildingId)
        }
        val trimmedNumber = room.roomNumber.trim()
        if (trimmedNumber.isEmpty()) {
            throw IllegalArgumentException("房间号不能为空")
        }
        val existingRoom = roomRepository.getById(room.id)
        val floor = if (building.buildingKind == BuildingKind.SHOP) {
            RoomGenerator.inferFloorFromRoomNumber(trimmedNumber)
        } else {
            room.floor
        }
        roomRepository.updateRoom(
            room = room.copy(
                roomNumber = trimmedNumber,
                floor = floor,
                updatedAt = System.currentTimeMillis(),
            ),
            featureTagIds = featureTagIds,
        )
        val isBottomShop = building.buildingKind == BuildingKind.RESIDENTIAL &&
            room.roomType == RoomType.SHOP &&
            room.floor == 0
        if (isBottomShop && existingRoom != null && existingRoom.roomNumber != trimmedNumber) {
            buildingRepository.updateBuilding(
                building.copy(
                    shopRooms = ShopRoomsFormatter.replace(
                        building.shopRooms,
                        existingRoom.roomNumber,
                        trimmedNumber,
                    ),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }
}
