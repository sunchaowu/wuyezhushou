package com.guanjiazhushou.assistant.data.repository

import com.guanjiazhushou.assistant.data.local.dao.RoomEventDao
import com.guanjiazhushou.assistant.data.local.mapper.toDomain
import com.guanjiazhushou.assistant.data.local.mapper.toEntity
import com.guanjiazhushou.assistant.domain.model.RoomEvent
import com.guanjiazhushou.assistant.domain.model.RoomEventStatus
import com.guanjiazhushou.assistant.domain.repository.RoomEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomEventRepositoryImpl @Inject constructor(
    private val roomEventDao: RoomEventDao,
) : RoomEventRepository {
    override fun observeByBuilding(buildingId: Long): Flow<List<RoomEvent>> =
        roomEventDao.observeByBuilding(buildingId).map { list -> list.map { it.toDomain() } }

    override fun observeByRoom(roomId: Long): Flow<List<RoomEvent>> =
        roomEventDao.observeByRoom(roomId).map { list -> list.map { it.toDomain() } }

    override fun observeAll(status: RoomEventStatus?): Flow<List<RoomEvent>> =
        roomEventDao.observeAll(status?.key).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): RoomEvent? =
        roomEventDao.getById(id)?.toDomain()

    override suspend fun insert(event: RoomEvent): Long =
        roomEventDao.insert(event.toEntity())

    override suspend fun update(event: RoomEvent) {
        roomEventDao.update(event.toEntity())
    }

    override suspend fun delete(id: Long) {
        roomEventDao.deleteById(id)
    }
}
