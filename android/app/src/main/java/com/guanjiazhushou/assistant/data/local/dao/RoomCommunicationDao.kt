package com.guanjiazhushou.assistant.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.guanjiazhushou.assistant.data.local.entity.RoomCommunicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomCommunicationDao {
    @Query(
        """
        SELECT * FROM room_communications
        WHERE room_id = :roomId
        ORDER BY created_at ASC
        """,
    )
    fun observeByRoom(roomId: Long): Flow<List<RoomCommunicationEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: RoomCommunicationEntity): Long
}
