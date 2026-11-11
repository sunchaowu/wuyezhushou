package com.guanjiazhushou.assistant.domain.usecase

import com.guanjiazhushou.assistant.domain.model.RoomEvent
import com.guanjiazhushou.assistant.domain.model.RoomEventSource
import com.guanjiazhushou.assistant.domain.model.RoomEventStatus
import com.guanjiazhushou.assistant.domain.model.RoomEventType
import com.guanjiazhushou.assistant.domain.repository.RoomEventRepository
import javax.inject.Inject

class CreateRoomEventUseCase @Inject constructor(
    private val roomEventRepository: RoomEventRepository,
) {
    suspend operator fun invoke(
        buildingId: Long,
        roomId: Long,
        type: RoomEventType,
        summary: String,
        rawText: String? = null,
        source: RoomEventSource = RoomEventSource.MANUAL,
    ): Long {
        val now = System.currentTimeMillis()
        return roomEventRepository.insert(
            RoomEvent(
                buildingId = buildingId,
                roomId = roomId,
                type = type,
                summary = summary.trim(),
                rawText = rawText,
                status = RoomEventStatus.PENDING,
                source = source,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }
}
