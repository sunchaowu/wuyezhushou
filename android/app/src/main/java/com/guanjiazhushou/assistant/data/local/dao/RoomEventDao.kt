package com.guanjiazhushou.assistant.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.guanjiazhushou.assistant.data.local.entity.RoomEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomEventDao {
    @Query("SELECT * FROM room_events WHERE id = :id")
    suspend fun getById(id: Long): RoomEventEntity?

    @Query("SELECT * FROM room_events WHERE building_id = :buildingId ORDER BY updated_at DESC")
    fun observeByBuilding(buildingId: Long): Flow<List<RoomEventEntity>>

    @Query("SELECT * FROM room_events WHERE room_id = :roomId ORDER BY updated_at DESC")
    fun observeByRoom(roomId: Long): Flow<List<RoomEventEntity>>

    @Query(
        """
        SELECT * FROM room_events
        WHERE (:status IS NULL OR status = :status)
        ORDER BY updated_at DESC
        """,
    )
    fun observeAll(status: String? = null): Flow<List<RoomEventEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: RoomEventEntity): Long

    @Update
    suspend fun update(entity: RoomEventEntity)

    @Query("DELETE FROM room_events WHERE id = :id")
    suspend fun deleteById(id: Long)
}
