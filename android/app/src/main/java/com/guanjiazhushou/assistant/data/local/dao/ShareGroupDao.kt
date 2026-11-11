package com.guanjiazhushou.assistant.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.guanjiazhushou.assistant.data.local.entity.ShareGroupBuildingEntity
import com.guanjiazhushou.assistant.data.local.entity.ShareGroupEntity
import com.guanjiazhushou.assistant.data.local.entity.ShareGroupMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShareGroupDao {
    @Query("SELECT * FROM share_groups ORDER BY created_at DESC")
    fun observeAll(): Flow<List<ShareGroupEntity>>

    @Query("SELECT * FROM share_groups WHERE id = :id")
    suspend fun getById(id: Long): ShareGroupEntity?

    @Query("SELECT * FROM share_groups WHERE status = 'active' LIMIT 1")
    suspend fun getActiveGroup(): ShareGroupEntity?

    @Query("SELECT COUNT(*) FROM share_groups WHERE status = 'active'")
    suspend fun countActiveGroups(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: ShareGroupEntity): Long

    @Update
    suspend fun updateGroup(group: ShareGroupEntity)

    @Delete
    suspend fun deleteGroup(group: ShareGroupEntity)

    @Query(
        """
        UPDATE share_groups
        SET status = 'dissolved', dissolved_at = :dissolvedAt
        WHERE id = :groupId
        """,
    )
    suspend fun dissolveGroup(groupId: Long, dissolvedAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: ShareGroupMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<ShareGroupMemberEntity>)

    @Query("SELECT * FROM share_group_members WHERE group_id = :groupId ORDER BY joined_at ASC")
    suspend fun getMembers(groupId: Long): List<ShareGroupMemberEntity>

    @Query(
        """
        UPDATE share_group_members
        SET left_at = :leftAt
        WHERE group_id = :groupId AND member_device_name = :memberDeviceName
        """,
    )
    suspend fun markMemberLeft(groupId: Long, memberDeviceName: String, leftAt: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSharedBuilding(shared: ShareGroupBuildingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSharedBuildings(shared: List<ShareGroupBuildingEntity>)

    @Query("SELECT * FROM share_group_buildings WHERE group_id = :groupId ORDER BY shared_at DESC")
    suspend fun getSharedBuildings(groupId: Long): List<ShareGroupBuildingEntity>

    @Query("DELETE FROM share_group_buildings WHERE group_id = :groupId AND building_id = :buildingId")
    suspend fun removeSharedBuilding(groupId: Long, buildingId: Long): Int

    @Transaction
    suspend fun dissolveAndCleanup(groupId: Long, dissolvedAt: Long) {
        dissolveGroup(groupId, dissolvedAt)
    }
}
