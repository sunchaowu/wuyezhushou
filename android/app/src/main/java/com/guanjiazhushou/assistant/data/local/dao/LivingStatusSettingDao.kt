package com.guanjiazhushou.assistant.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.guanjiazhushou.assistant.data.local.entity.LivingStatusSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LivingStatusSettingDao {
    @Query("SELECT * FROM living_status_settings ORDER BY sort_order ASC, status_key ASC")
    fun observeAll(): Flow<List<LivingStatusSettingEntity>>

    @Query("SELECT * FROM living_status_settings ORDER BY sort_order ASC, status_key ASC")
    suspend fun getAll(): List<LivingStatusSettingEntity>

    @Query("SELECT COUNT(*) FROM living_status_settings WHERE label = :label")
    suspend fun countByLabel(label: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM living_status_settings
        WHERE label = :label AND status_key != :excludeStatusKey
        """,
    )
    suspend fun countByLabelExcluding(label: String, excludeStatusKey: String): Int

    @Query("SELECT COALESCE(MAX(sort_order), 0) FROM living_status_settings")
    suspend fun getMaxSortOrder(): Int

    @Query(
        """
        DELETE FROM living_status_settings
        WHERE status_key = :statusKey AND is_builtin = 0
        """,
    )
    suspend fun deleteCustom(statusKey: String): Int

    @Query("SELECT * FROM living_status_settings WHERE status_key = :statusKey")
    suspend fun getByKey(statusKey: String): LivingStatusSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: LivingStatusSettingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(settings: List<LivingStatusSettingEntity>)

    @Update
    suspend fun update(setting: LivingStatusSettingEntity)

    @Query(
        """
        UPDATE living_status_settings
        SET counts_as_occupied = :countsAsOccupied
        WHERE status_key = :statusKey
        """,
    )
    suspend fun updateCountsAsOccupied(statusKey: String, countsAsOccupied: Boolean): Int
}
