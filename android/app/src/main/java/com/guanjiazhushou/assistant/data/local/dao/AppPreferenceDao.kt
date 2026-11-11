package com.guanjiazhushou.assistant.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.guanjiazhushou.assistant.data.local.entity.AppPreferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppPreferenceDao {
    @Query("SELECT * FROM app_preferences WHERE key = :key")
    suspend fun getByKey(key: String): AppPreferenceEntity?

    @Query("SELECT value FROM app_preferences WHERE key = :key")
    suspend fun getValue(key: String): String?

    @Query("SELECT value FROM app_preferences WHERE key = :key")
    fun observeValue(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preference: AppPreferenceEntity)

    @Query("DELETE FROM app_preferences WHERE key = :key")
    suspend fun deleteByKey(key: String): Int
}
