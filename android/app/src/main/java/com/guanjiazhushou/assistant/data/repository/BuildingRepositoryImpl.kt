package com.guanjiazhushou.assistant.data.repository

import androidx.room.withTransaction
import com.guanjiazhushou.assistant.data.local.AppDatabase
import com.guanjiazhushou.assistant.data.local.dao.BuildingDao
import com.guanjiazhushou.assistant.data.local.dao.RoomDao
import com.guanjiazhushou.assistant.data.local.mapper.toDomain
import com.guanjiazhushou.assistant.data.local.mapper.toDomainBuildings
import com.guanjiazhushou.assistant.data.local.mapper.toDomainRooms
import com.guanjiazhushou.assistant.data.local.mapper.toEntity
import com.guanjiazhushou.assistant.domain.exception.ReadOnlyBuildingException
import com.guanjiazhushou.assistant.domain.model.Building
import com.guanjiazhushou.assistant.domain.model.BuildingKind
import com.guanjiazhushou.assistant.domain.model.PaymentType
import com.guanjiazhushou.assistant.domain.model.Room
import com.guanjiazhushou.assistant.domain.model.RoomType
import com.guanjiazhushou.assistant.domain.util.ShopRoomsFormatter
import com.guanjiazhushou.assistant.domain.usecase.GenerateRoomsUseCase
import com.guanjiazhushou.assistant.domain.repository.BuildingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildingRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val buildingDao: BuildingDao,
    private val roomDao: RoomDao,
    private val generateRoomsUseCase: GenerateRoomsUseCase,
) : BuildingRepository {
    override fun observeAll(): Flow<List<Building>> =
        buildingDao.observeAll().map { it.toDomainBuildings() }

    override fun observeByCommunity(communityId: Long): Flow<List<Building>> =
        buildingDao.observeByCommunity(communityId).map { it.toDomainBuildings() }

    override suspend fun getById(id: Long): Building? =
        buildingDao.getById(id)?.toDomain()

    override suspend fun getLocalBuildings(): List<Building> =
        buildingDao.getLocalBuildings().toDomainBuildings()

    override suspend fun getImportedBuildings(): List<Building> =
        buildingDao.getImportedBuildings().toDomainBuildings()

    override suspend fun getLocalBuildingIds(): List<Long> =
        buildingDao.getLocalBuildingIds()

    override suspend fun getAllBuildingIds(): List<Long> =
        buildingDao.getAllBuildingIds()

    override suspend fun createBuildingWithRooms(building: Building, rooms: List<Room>): Long =
        database.withTransaction {
            val buildingId = buildingDao.insert(building.toEntity())
            val roomEntities = rooms.map { room ->
                room.copy(buildingId = buildingId).toEntity()
            }
            roomDao.insertAll(roomEntities)
            buildingId
        }

    override suspend fun updateBuilding(building: Building) {
        buildingDao.update(building.toEntity())
    }

    override suspend fun updateBuildingWithRoomSync(
        building: Building,
        defaultPaymentType: PaymentType,
    ) {
        database.withTransaction {
            val now = System.currentTimeMillis()
            buildingDao.update(building.copy(updatedAt = now).toEntity())

            val existing = roomDao.getByBuilding(building.id).toDomainRooms()
            val existingByNumber = existing.associateBy { it.roomNumber }
            val generated = generateRoomsUseCase(
                buildingId = building.id,
                buildingKind = building.buildingKind,
                startFloor = building.startFloor,
                maxFloor = building.maxFloor,
                unitsPerFloor = building.unitsPerFloor,
                refugeFloors = building.refugeFloors,
                excludeRooms = building.excludeRooms,
                shopRooms = building.shopRooms,
                defaultPaymentType = defaultPaymentType,
                timestamp = now,
            )
            val generatedNumbers = generated.map { it.roomNumber }.toSet()

            val toInsert = generated
                .filter { it.roomNumber !in existingByNumber }
                .map { it.toEntity() }
            if (toInsert.isNotEmpty()) {
                roomDao.insertAll(toInsert)
            }

            val toDeleteIds = existing
                .filter { it.roomNumber !in generatedNumbers }
                .map { it.id }
            if (toDeleteIds.isNotEmpty()) {
                roomDao.deleteByIds(toDeleteIds)
            }
        }
    }

    override suspend fun deleteBuilding(id: Long) {
        database.withTransaction {
            roomDao.deleteByBuilding(id)
            buildingDao.deleteById(id)
        }
    }

    override suspend fun findByCommunityAndName(communityId: Long, name: String): Building? =
        buildingDao.findByCommunityAndName(communityId, name)?.toDomain()

    override suspend fun addBottomShopRoom(buildingId: Long, roomNumber: String): Long =
        database.withTransaction {
            val building = buildingDao.getById(buildingId)?.toDomain()
                ?: throw IllegalArgumentException("楼栋不存在")
            if (building.isReadonly) {
                throw ReadOnlyBuildingException(buildingId)
            }
            if (building.buildingKind != BuildingKind.RESIDENTIAL) {
                throw IllegalArgumentException("仅住宅楼栋支持底部商铺")
            }
            val trimmed = roomNumber.trim()
            if (trimmed.isEmpty()) {
                throw IllegalArgumentException("商铺编号不能为空")
            }
            if (roomDao.countByBuildingAndRoomNumber(buildingId, trimmed, excludeRoomId = -1) > 0) {
                throw IllegalArgumentException("房间号 $trimmed 已存在")
            }
            val now = System.currentTimeMillis()
            val roomId = roomDao.insert(
                Room(
                    buildingId = buildingId,
                    floor = 0,
                    roomNumber = trimmed,
                    roomType = RoomType.SHOP,
                    paymentType = PaymentType.ARREARS,
                    paymentMonths = 1,
                    updatedAt = now,
                ).toEntity(),
            )
            buildingDao.update(
                building.copy(
                    shopRooms = ShopRoomsFormatter.append(building.shopRooms, trimmed),
                    updatedAt = now,
                ).toEntity(),
            )
            roomId
        }

    override suspend fun removeBottomShopRoom(buildingId: Long, roomId: Long) {
        val building = buildingDao.getById(buildingId)?.toDomain()
            ?: throw IllegalArgumentException("楼栋不存在")
        val room = roomDao.getById(roomId)?.toDomain()
            ?: throw IllegalArgumentException("铺位不存在")
        deleteRooms(building, listOf(room))
    }

    override suspend fun deleteRooms(building: Building, rooms: List<Room>) {
        if (rooms.isEmpty()) return
        database.withTransaction {
            if (building.isReadonly) {
                throw ReadOnlyBuildingException(building.id)
            }
            val buildingRooms = rooms.filter { it.buildingId == building.id }
            if (buildingRooms.isEmpty()) return@withTransaction

            var shopRooms = building.shopRooms
            when (building.buildingKind) {
                BuildingKind.RESIDENTIAL -> {
                    buildingRooms
                        .filter { it.roomType == RoomType.SHOP && it.floor == 0 }
                        .forEach { room ->
                            shopRooms = ShopRoomsFormatter.remove(shopRooms, room.roomNumber)
                        }
                }
                BuildingKind.SHOP -> {
                    buildingRooms.forEach { room ->
                        shopRooms = ShopRoomsFormatter.remove(shopRooms, room.roomNumber)
                    }
                }
            }

            roomDao.deleteByIds(buildingRooms.map { it.id })
            val now = System.currentTimeMillis()
            if (shopRooms != building.shopRooms) {
                buildingDao.update(
                    building.copy(shopRooms = shopRooms, updatedAt = now).toEntity(),
                )
            }
        }
    }
}
