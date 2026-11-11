package com.guanjiazhushou.assistant.domain.repository

import com.guanjiazhushou.assistant.domain.model.AppThemeId
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    fun observeThemeId(): Flow<AppThemeId>

    suspend fun getThemeId(): AppThemeId

    suspend fun setThemeId(themeId: AppThemeId)
}
