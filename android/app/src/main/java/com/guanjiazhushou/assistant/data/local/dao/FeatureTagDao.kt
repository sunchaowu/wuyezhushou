package com.guanjiazhushou.assistant.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.guanjiazhushou.assistant.data.local.entity.FeatureTagEntity
import com.guanjiazhushou.assistant.data.local.entity.RoomFeatureCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface FeatureTagDao {
    @Query("SELECT * FROM feature_tags ORDER BY sort_order ASC, name ASC")
    fun observeAll(): Flow<List<FeatureTagEntity>>

    @Query("SELECT * FROM feature_tags ORDER BY sort_order ASC, name ASC")
    suspend fun getAll(): List<FeatureTagEntity>

    @Query("SELECT * FROM feature_tags WHERE id = :id")
    suspend fun getById(id: Long): FeatureTagEntity?

    @Query("SELECT COUNT(*) FROM feature_tags WHERE name = :name COLLATE NOCASE")
    suspend fun countByName(name: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM feature_tags
        WHERE name = :name COLLATE NOCASE AND id != :excludeId
        """,
    )
    suspend fun countByNameExcluding(name: String, excludeId: Long): Int

    @Query("SELECT * FROM feature_tags WHERE is_builtin = 1 ORDER BY sort_order ASC")
    suspend fun getBuiltinTags(): List<FeatureTagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: FeatureTagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<FeatureTagEntity>): List<Long>

    @Update
    suspend fun update(tag: FeatureTagEntity)

    @Delete
    suspend fun delete(tag: FeatureTagEntity)

    @Query("DELETE FROM feature_tags WHERE id = :id AND is_builtin = 0")
    suspend fun deleteCustomById(id: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkRoomFeature(crossRef: RoomFeatureCrossRef)

    @Query("DELETE FROM room_features WHERE feature_tag_id = :featureTagId")
    suspend fun unlinkAllRooms(featureTagId: Long): Int
}
