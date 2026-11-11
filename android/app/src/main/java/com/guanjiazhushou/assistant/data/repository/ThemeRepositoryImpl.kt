package com.guanjiazhushou.assistant.data.repository

import com.guanjiazhushou.assistant.data.local.dao.AppPreferenceDao
import com.guanjiazhushou.assistant.data.local.entity.AppPreferenceEntity
import com.guanjiazhushou.assistant.domain.model.AppThemeId
import com.guanjiazhushou.assistant.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeRepositoryImpl @Inject constructor(
    private val appPreferenceDao: AppPreferenceDao,
) : ThemeRepository {
    override fun observeThemeId(): Flow<AppThemeId> =
        appPreferenceDao.observeValue(KEY_THEME_ID).map { value ->
            AppThemeId.fromKey(value ?: AppThemeId.BLUE_BUSINESS.key)
                ?: AppThemeId.BLUE_BUSINESS
        }

    override suspend fun getThemeId(): AppThemeId {
        val value = appPreferenceDao.getValue(KEY_THEME_ID)
        return AppThemeId.fromKey(value ?: AppThemeId.BLUE_BUSINESS.key)
            ?: AppThemeId.BLUE_BUSINESS
    }

    override suspend fun setThemeId(themeId: AppThemeId) {
        appPreferenceDao.upsert(
            AppPreferenceEntity(key = KEY_THEME_ID, value = themeId.key),
        )
    }

    companion object {
        const val KEY_THEME_ID = "theme_id"
    }
}
