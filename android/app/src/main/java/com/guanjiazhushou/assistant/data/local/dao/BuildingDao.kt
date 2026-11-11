package com.guanjiazhushou.assistant.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.guanjiazhushou.assistant.data.local.entity.BuildingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BuildingDao {
    @Query("SELECT * FROM buildings ORDER BY name ASC")
    fun observeAll(): Flow<List<BuildingEntity>>

    @Query("SELECT * FROM buildings WHERE community_id = :communityId ORDER BY name ASC")
    fun observeByCommunity(communityId: Long): Flow<List<BuildingEntity>>

    @Query("SELECT * FROM buildings WHERE id = :id")
    suspend fun getById(id: Long): BuildingEntity?

    @Query("SELECT * FROM buildings WHERE source_type = 'local' ORDER BY name ASC")
    suspend fun getLocalBuildings(): List<BuildingEntity>

    @Query("SELECT * FROM buildings WHERE source_type = 'imported' ORDER BY name ASC")
    suspend fun getImportedBuildings(): List<BuildingEntity>

    @Query("SELECT id FROM buildings WHERE source_type = 'local'")
    suspend fun getLocalBuildingIds(): List<Long>

    @Query("SELECT id FROM buildings")
    suspend fun getAllBuildingIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(building: BuildingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(buildings: List<BuildingEntity>): List<Long>

    @Update
    suspend fun update(building: BuildingEntity)

    @Delete
    suspend fun delete(building: BuildingEntity)

    @Query("DELETE FROM buildings WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM buildings WHERE community_id = :communityId AND name = :name LIMIT 1")
    suspend fun findByCommunityAndName(communityId: Long, name: String): BuildingEntity?
}
