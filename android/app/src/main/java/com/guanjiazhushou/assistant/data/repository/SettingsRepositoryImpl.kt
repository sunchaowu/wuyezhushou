package com.guanjiazhushou.assistant.data.repository

import com.guanjiazhushou.assistant.data.local.dao.AppPreferenceDao
import com.guanjiazhushou.assistant.data.local.dao.LivingStatusSettingDao
import com.guanjiazhushou.assistant.data.local.dao.RoomDao
import com.guanjiazhushou.assistant.data.local.entity.AppPreferenceEntity
import com.guanjiazhushou.assistant.data.local.mapper.toDomainLivingStatusSettings
import com.guanjiazhushou.assistant.data.local.mapper.toEntity
import com.guanjiazhushou.assistant.domain.model.LivingStatusSetting
import com.guanjiazhushou.assistant.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val livingStatusSettingDao: LivingStatusSettingDao,
    private val roomDao: RoomDao,
    private val appPreferenceDao: AppPreferenceDao,
) : SettingsRepository {
    override fun observeLivingStatusSettings(): Flow<List<LivingStatusSetting>> =
        livingStatusSettingDao.observeAll().map { it.toDomainLivingStatusSettings() }

    override suspend fun getLivingStatusSettings(): List<LivingStatusSetting> =
        livingStatusSettingDao.getAll().toDomainLivingStatusSettings()

    override suspend fun updateCountsAsOccupied(statusKey: String, countsAsOccupied: Boolean) {
        livingStatusSettingDao.updateCountsAsOccupied(statusKey, countsAsOccupied)
    }

    override suspend fun updateLivingStatus(setting: LivingStatusSetting) {
        livingStatusSettingDao.update(setting.toEntity())
    }

    override suspend fun insertLivingStatus(setting: LivingStatusSetting) {
        livingStatusSettingDao.insert(setting.toEntity())
    }

    override suspend fun existsByLabel(label: String, excludeStatusKey: String?): Boolean {
        val trimmed = label.trim()
        return if (excludeStatusKey == null) {
            livingStatusSettingDao.countByLabel(trimmed) > 0
        } else {
            livingStatusSettingDao.countByLabelExcluding(trimmed, excludeStatusKey) > 0
        }
    }

    override suspend fun deleteCustomLivingStatus(statusKey: String): Boolean =
        livingStatusSettingDao.deleteCustom(statusKey) > 0

    override suspend fun countRoomsUsingStatus(statusKey: String): Int =
        roomDao.countByLivingStatus(statusKey)

    override suspend fun getPreference(key: String): String? =
        appPreferenceDao.getValue(key)

    override suspend fun setPreference(key: String, value: String) {
        appPreferenceDao.upsert(AppPreferenceEntity(key = key, value = value))
    }
}
