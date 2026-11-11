package com.guanjiazhushou.assistant.domain.usecase

import com.guanjiazhushou.assistant.domain.model.RoomEvent
import com.guanjiazhushou.assistant.domain.model.RoomEventStatus
import com.guanjiazhushou.assistant.domain.repository.RoomEventRepository
import javax.inject.Inject

class UpdateRoomEventUseCase @Inject constructor(
    private val roomEventRepository: RoomEventRepository,
) {
    suspend operator fun invoke(event: RoomEvent) {
        val now = System.currentTimeMillis()
        val completedAt = when (event.status) {
            RoomEventStatus.DONE, RoomEventStatus.CANCELLED -> event.completedAt ?: now
            else -> null
        }
        roomEventRepository.update(
            event.copy(
                updatedAt = now,
                completedAt = completedAt,
            ),
        )
    }
}
