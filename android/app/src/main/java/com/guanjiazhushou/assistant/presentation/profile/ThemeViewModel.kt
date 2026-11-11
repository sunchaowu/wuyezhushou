package com.guanjiazhushou.assistant.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guanjiazhushou.assistant.domain.model.AppThemeId
import com.guanjiazhushou.assistant.domain.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
) : ViewModel() {

    val themeId: StateFlow<AppThemeId> = themeRepository.observeThemeId()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppThemeId.BLUE_BUSINESS,
        )

    fun setTheme(themeId: AppThemeId) {
        viewModelScope.launch {
            themeRepository.setThemeId(themeId)
        }
    }
}
