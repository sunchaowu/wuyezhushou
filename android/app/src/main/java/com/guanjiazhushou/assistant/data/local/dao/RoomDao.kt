package com.guanjiazhushou.assistant.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.guanjiazhushou.assistant.data.local.entity.RoomEntity
import com.guanjiazhushou.assistant.data.local.entity.RoomFeatureCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms WHERE building_id = :buildingId ORDER BY floor ASC, room_number ASC")
    fun observeByBuilding(buildingId: Long): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE building_id = :buildingId ORDER BY floor ASC, room_number ASC")
    suspend fun getByBuilding(buildingId: Long): List<RoomEntity>

    @Query("SELECT * FROM rooms WHERE id = :id")
    suspend fun getById(id: Long): RoomEntity?

    @Query(
        """
        SELECT COUNT(*) FROM rooms
        WHERE building_id = :buildingId AND room_number = :roomNumber AND id != :excludeRoomId
        """,
    )
    suspend fun countByBuildingAndRoomNumber(
        buildingId: Long,
        roomNumber: String,
        excludeRoomId: Long,
    ): Int

    @Query(
        """
        SELECT * FROM rooms
        WHERE building_id IN (:buildingIds)
        ORDER BY building_id ASC, floor ASC, room_number ASC
        """,
    )
    suspend fun getByBuildingIds(buildingIds: List<Long>): List<RoomEntity>

    @Query(
        """
        SELECT * FROM rooms
        WHERE building_id IN (:buildingIds) AND living_status = :livingStatus
        ORDER BY building_id ASC, floor ASC, room_number ASC
        """,
    )
    suspend fun getByLivingStatus(buildingIds: List<Long>, livingStatus: String): List<RoomEntity>

    @Query("SELECT COUNT(*) FROM rooms WHERE living_status = :livingStatus")
    suspend fun countByLivingStatus(livingStatus: String): Int

    @Query(
        """
        SELECT r.* FROM rooms r
        INNER JOIN living_status_settings lss ON r.living_status = lss.status_key
        WHERE lss.counts_as_occupied = 1
          AND r.building_id IN (:buildingIds)
        ORDER BY r.building_id ASC, r.floor ASC, r.room_number ASC
        """,
    )
    suspend fun getOccupiedRooms(buildingIds: List<Long>): List<RoomEntity>

    @Query(
        """
        SELECT * FROM rooms
        WHERE building_id IN (:buildingIds) AND payment_type IN ('prepaid', 'normal')
        ORDER BY building_id ASC, floor ASC, room_number ASC
        """,
    )
    suspend fun getCollectedRooms(buildingIds: List<Long>): List<RoomEntity>

    @Query(
        """
        SELECT * FROM rooms
        WHERE building_id IN (:buildingIds) AND payment_type = 'arrears'
        ORDER BY building_id ASC, floor ASC, room_number ASC
        """,
    )
    suspend fun getArrearsRooms(buildingIds: List<Long>): List<RoomEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(room: RoomEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rooms: List<RoomEntity>): List<Long>

    @Update
    suspend fun update(room: RoomEntity)

    @Delete
    suspend fun delete(room: RoomEntity)

    @Query("DELETE FROM rooms WHERE building_id = :buildingId")
    suspend fun deleteByBuilding(buildingId: Long): Int

    @Query("DELETE FROM rooms WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>): Int

    @Query(
        """
        UPDATE rooms
        SET payment_type = :paymentType,
            payment_months = :paymentMonths,
            updated_at = :updatedAt
        WHERE id IN (:roomIds)
        """,
    )
    suspend fun updatePaymentTypeByIds(
        roomIds: List<Long>,
        paymentType: String,
        paymentMonths: Int,
        updatedAt: Long,
    ): Int

    @Query(
        """
        UPDATE rooms
        SET living_status = :livingStatusKey,
            updated_at = :updatedAt
        WHERE id IN (:roomIds)
        """,
    )
    suspend fun updateLivingStatusByIds(
        roomIds: List<Long>,
        livingStatusKey: String,
        updatedAt: Long,
    ): Int

    @Query(
        """
        UPDATE rooms
        SET updated_at = :updatedAt
        WHERE id IN (:roomIds)
        """,
    )
    suspend fun touchUpdatedAtByIds(roomIds: List<Long>, updatedAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoomFeatures(crossRefs: List<RoomFeatureCrossRef>)

    @Query("DELETE FROM room_features WHERE room_id = :roomId")
    suspend fun clearRoomFeatures(roomId: Long): Int

    @Query("SELECT feature_tag_id FROM room_features WHERE room_id = :roomId")
    suspend fun getFeatureTagIdsForRoom(roomId: Long): List<Long>

    @Transaction
    suspend fun replaceRoomFeatures(roomId: Long, crossRefs: List<RoomFeatureCrossRef>) {
        clearRoomFeatures(roomId)
        if (crossRefs.isNotEmpty()) {
            insertRoomFeatures(crossRefs)
        }
    }
}
