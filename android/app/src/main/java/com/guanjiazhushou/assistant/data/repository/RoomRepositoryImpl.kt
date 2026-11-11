package com.guanjiazhushou.assistant.data.repository

import com.guanjiazhushou.assistant.data.local.dao.BuildingDao
import com.guanjiazhushou.assistant.data.local.dao.RoomDao
import com.guanjiazhushou.assistant.data.local.entity.RoomFeatureCrossRef
import com.guanjiazhushou.assistant.data.local.mapper.toDomain
import com.guanjiazhushou.assistant.data.local.mapper.toDomainRooms
import com.guanjiazhushou.assistant.data.local.mapper.toEntity
import com.guanjiazhushou.assistant.domain.model.BatchFeatureTagMode
import com.guanjiazhushou.assistant.domain.model.LivingStatus
import com.guanjiazhushou.assistant.domain.model.PaymentType
import com.guanjiazhushou.assistant.domain.model.Room
import com.guanjiazhushou.assistant.domain.model.RoomListFilter
import com.guanjiazhushou.assistant.domain.model.RoomWithFeatureIds
import com.guanjiazhushou.assistant.domain.model.StatisticsScope
import com.guanjiazhushou.assistant.domain.repository.RoomRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class RoomRepositoryImpl @Inject constructor(
    private val roomDao: RoomDao,
    private val buildingDao: BuildingDao,
) : RoomRepository {

    override fun observeByBuilding(buildingId: Long): Flow<List<Room>> =
        roomDao.observeByBuilding(buildingId).map { it.toDomainRooms() }

    override fun observeWithFeatures(buildingId: Long): Flow<List<RoomWithFeatureIds>> =
        roomDao.observeByBuilding(buildingId)
            .mapLatest { rooms ->
                rooms.map { entity ->
                    RoomWithFeatureIds(
                        room = entity.toDomain(),
                        featureTagIds = roomDao.getFeatureTagIdsForRoom(entity.id),
                    )
                }
            }
            .flowOn(Dispatchers.IO)

    override suspend fun getById(id: Long): Room? =
        roomDao.getById(id)?.toDomain()

    override suspend fun getByBuilding(buildingId: Long): List<Room> =
        roomDao.getByBuilding(buildingId).toDomainRooms()

    override suspend fun getFeatureTagIdsForRoom(roomId: Long): List<Long> =
        roomDao.getFeatureTagIdsForRoom(roomId)

    override suspend fun updateRoom(room: Room, featureTagIds: List<Long>) {
        val duplicateCount = roomDao.countByBuildingAndRoomNumber(
            buildingId = room.buildingId,
            roomNumber = room.roomNumber.trim(),
            excludeRoomId = room.id,
        )
        if (duplicateCount > 0) {
            throw IllegalArgumentException("房间号 ${room.roomNumber} 已存在")
        }
        roomDao.update(room.toEntity())
        val crossRefs = featureTagIds.map { tagId ->
            RoomFeatureCrossRef(roomId = room.id, featureTagId = tagId)
        }
        roomDao.replaceRoomFeatures(room.id, crossRefs)
    }

    override suspend fun batchUpdatePaymentType(
        roomIds: List<Long>,
        paymentType: PaymentType,
        paymentMonths: Int,
    ) {
        if (roomIds.isEmpty()) return
        roomDao.updatePaymentTypeByIds(
            roomIds = roomIds,
            paymentType = paymentType.key,
            paymentMonths = paymentMonths,
            updatedAt = System.currentTimeMillis(),
        )
    }

    override suspend fun batchUpdateLivingStatus(
        roomIds: List<Long>,
        livingStatusKey: String,
    ) {
        if (roomIds.isEmpty()) return
        roomDao.updateLivingStatusByIds(
            roomIds = roomIds,
            livingStatusKey = livingStatusKey,
            updatedAt = System.currentTimeMillis(),
        )
    }

    override suspend fun batchUpdateFeatureTags(
        roomIds: List<Long>,
        mode: BatchFeatureTagMode,
        tagIds: List<Long>,
    ) {
        if (roomIds.isEmpty() || tagIds.isEmpty()) return
        val tagIdSet = tagIds.toSet()
        roomIds.forEach { roomId ->
            val current = roomDao.getFeatureTagIdsForRoom(roomId).toSet()
            val updated = when (mode) {
                BatchFeatureTagMode.REPLACE -> tagIdSet
                BatchFeatureTagMode.ADD -> current + tagIdSet
                BatchFeatureTagMode.REMOVE -> current - tagIdSet
            }
            val crossRefs = updated.map { tagId ->
                RoomFeatureCrossRef(roomId = roomId, featureTagId = tagId)
            }
            roomDao.replaceRoomFeatures(roomId, crossRefs)
        }
        roomDao.touchUpdatedAtByIds(roomIds, System.currentTimeMillis())
    }

    override suspend fun getRoomsForFilter(
        scope: StatisticsScope,
        filter: RoomListFilter,
    ): List<Room> {
        val buildingIds = when (scope) {
            StatisticsScope.MINE -> buildingDao.getLocalBuildingIds()
            StatisticsScope.OVERALL -> buildingDao.getAllBuildingIds()
        }
        if (buildingIds.isEmpty()) return emptyList()
        val entities = when (filter) {
            RoomListFilter.TOTAL -> roomDao.getByBuildingIds(buildingIds)
            RoomListFilter.OCCUPIED -> roomDao.getOccupiedRooms(buildingIds)
            RoomListFilter.ARREARS -> roomDao.getArrearsRooms(buildingIds)
            RoomListFilter.COLLECTED -> roomDao.getCollectedRooms(buildingIds)
            RoomListFilter.RENTED -> roomDao.getByLivingStatus(
                buildingIds,
                LivingStatus.RENT_RENTED.key,
            )
        }
        return entities.toDomainRooms()
    }
}
