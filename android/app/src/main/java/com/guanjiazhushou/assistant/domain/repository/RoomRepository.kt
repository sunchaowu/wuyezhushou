package com.guanjiazhushou.assistant.domain.repository

import com.guanjiazhushou.assistant.domain.model.BatchFeatureTagMode
import com.guanjiazhushou.assistant.domain.model.PaymentType
import com.guanjiazhushou.assistant.domain.model.Room
import com.guanjiazhushou.assistant.domain.model.RoomListFilter
import com.guanjiazhushou.assistant.domain.model.RoomWithFeatureIds
import com.guanjiazhushou.assistant.domain.model.StatisticsScope
import kotlinx.coroutines.flow.Flow

interface RoomRepository {
    fun observeByBuilding(buildingId: Long): Flow<List<Room>>
    fun observeWithFeatures(buildingId: Long): Flow<List<RoomWithFeatureIds>>
    suspend fun getById(id: Long): Room?
    suspend fun getByBuilding(buildingId: Long): List<Room>
    suspend fun updateRoom(room: Room, featureTagIds: List<Long> = emptyList())
    suspend fun batchUpdatePaymentType(
        roomIds: List<Long>,
        paymentType: PaymentType,
        paymentMonths: Int,
    )
    suspend fun batchUpdateLivingStatus(
        roomIds: List<Long>,
        livingStatusKey: String,
    )
    suspend fun batchUpdateFeatureTags(
        roomIds: List<Long>,
        mode: BatchFeatureTagMode,
        tagIds: List<Long>,
    )
    suspend fun getFeatureTagIdsForRoom(roomId: Long): List<Long>
    suspend fun getRoomsForFilter(scope: StatisticsScope, filter: RoomListFilter): List<Room>
}
