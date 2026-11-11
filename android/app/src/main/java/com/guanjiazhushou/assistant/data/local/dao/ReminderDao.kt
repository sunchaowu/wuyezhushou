package com.guanjiazhushou.assistant.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.guanjiazhushou.assistant.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE status = 'scheduled' ORDER BY trigger_at ASC")
    suspend fun getScheduled(): List<ReminderEntity>

    @Query("SELECT * FROM reminders ORDER BY trigger_at DESC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query(
        """
        SELECT * FROM reminders
        WHERE target_type = :targetType AND target_id = :targetId
        ORDER BY trigger_at DESC
        """,
    )
    fun observeForTarget(targetType: String, targetId: Long): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ReminderEntity): Long

    @Update
    suspend fun update(entity: ReminderEntity)

    @Query("UPDATE reminders SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query(
        """
        UPDATE reminders SET status = 'cancelled'
        WHERE target_type = :targetType AND target_id = :targetId AND status = 'scheduled'
        """,
    )
    suspend fun cancelForTarget(targetType: String, targetId: Long)
}
