package com.guanjiazhushou.assistant.domain.usecase

import com.guanjiazhushou.assistant.domain.repository.BuildingRepository
import com.guanjiazhushou.assistant.domain.repository.RoomRepository
import javax.inject.Inject

class BatchDeleteRoomsUseCase @Inject constructor(
    private val buildingRepository: BuildingRepository,
    private val roomRepository: RoomRepository,
) {
    suspend operator fun invoke(buildingId: Long, roomIds: List<Long>) {
        if (roomIds.isEmpty()) return
        val building = buildingRepository.getById(buildingId)
            ?: throw IllegalArgumentException("楼栋不存在")
        val rooms = roomIds.mapNotNull { roomRepository.getById(it) }
            .filter { it.buildingId == buildingId }
        if (rooms.isEmpty()) return
        buildingRepository.deleteRooms(building, rooms)
    }
}
