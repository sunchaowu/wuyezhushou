package com.guanjiazhushou.assistant.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.guanjiazhushou.assistant.data.local.entity.CommunityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunityDao {
    @Query("SELECT * FROM communities ORDER BY name ASC")
    fun observeAll(): Flow<List<CommunityEntity>>

    @Query("SELECT * FROM communities ORDER BY name ASC")
    suspend fun getAll(): List<CommunityEntity>

    @Query("SELECT * FROM communities WHERE id = :id")
    suspend fun getById(id: Long): CommunityEntity?

    @Query("SELECT * FROM communities WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): CommunityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(community: CommunityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(communities: List<CommunityEntity>): List<Long>

    @Update
    suspend fun update(community: CommunityEntity)

    @Delete
    suspend fun delete(community: CommunityEntity)

    @Query("DELETE FROM communities WHERE id = :id")
    suspend fun deleteById(id: Long)
}
