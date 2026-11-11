package com.guanjiazhushou.assistant.domain.repository

import com.guanjiazhushou.assistant.domain.model.Building
import com.guanjiazhushou.assistant.domain.model.PaymentType
import com.guanjiazhushou.assistant.domain.model.Room
import kotlinx.coroutines.flow.Flow

interface BuildingRepository {
    fun observeAll(): Flow<List<Building>>
    fun observeByCommunity(communityId: Long): Flow<List<Building>>
    suspend fun getById(id: Long): Building?
    suspend fun getLocalBuildings(): List<Building>
    suspend fun getImportedBuildings(): List<Building>
    suspend fun getLocalBuildingIds(): List<Long>
    suspend fun getAllBuildingIds(): List<Long>
    suspend fun createBuildingWithRooms(building: Building, rooms: List<Room>): Long
    suspend fun updateBuilding(building: Building)
    suspend fun updateBuildingWithRoomSync(building: Building, defaultPaymentType: PaymentType)
    suspend fun deleteBuilding(id: Long)
    suspend fun findByCommunityAndName(communityId: Long, name: String): Building?
    suspend fun addBottomShopRoom(buildingId: Long, roomNumber: String): Long
    suspend fun removeBottomShopRoom(buildingId: Long, roomId: Long)
    suspend fun deleteRooms(building: Building, rooms: List<Room>)
}
