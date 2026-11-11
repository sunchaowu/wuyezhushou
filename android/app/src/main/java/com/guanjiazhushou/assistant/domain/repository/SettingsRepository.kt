package com.guanjiazhushou.assistant.domain.repository

import com.guanjiazhushou.assistant.domain.model.LivingStatusSetting
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeLivingStatusSettings(): Flow<List<LivingStatusSetting>>

    suspend fun getLivingStatusSettings(): List<LivingStatusSetting>

    suspend fun updateCountsAsOccupied(statusKey: String, countsAsOccupied: Boolean)

    suspend fun updateLivingStatus(setting: LivingStatusSetting)

    suspend fun insertLivingStatus(setting: LivingStatusSetting)

    suspend fun existsByLabel(label: String, excludeStatusKey: String? = null): Boolean

    suspend fun deleteCustomLivingStatus(statusKey: String): Boolean

    suspend fun countRoomsUsingStatus(statusKey: String): Int

    suspend fun getPreference(key: String): String?

    suspend fun setPreference(key: String, value: String)
}
