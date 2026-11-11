package com.guanjiazhushou.assistant.domain.repository

import com.guanjiazhushou.assistant.domain.model.RoomEvent
import com.guanjiazhushou.assistant.domain.model.RoomEventStatus
import kotlinx.coroutines.flow.Flow

interface RoomEventRepository {
    fun observeByBuilding(buildingId: Long): Flow<List<RoomEvent>>
    fun observeByRoom(roomId: Long): Flow<List<RoomEvent>>
    fun observeAll(status: RoomEventStatus? = null): Flow<List<RoomEvent>>
    suspend fun getById(id: Long): RoomEvent?
    suspend fun insert(event: RoomEvent): Long
    suspend fun update(event: RoomEvent)
    suspend fun delete(id: Long)
}
