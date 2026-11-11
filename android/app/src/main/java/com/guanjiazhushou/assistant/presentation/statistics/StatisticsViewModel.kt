package com.guanjiazhushou.assistant.presentation.statistics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guanjiazhushou.assistant.domain.model.LivingStatusSetting
import com.guanjiazhushou.assistant.domain.model.Room
import com.guanjiazhushou.assistant.domain.model.RoomListFilter
import com.guanjiazhushou.assistant.domain.model.StatisticsScope
import com.guanjiazhushou.assistant.domain.model.StatisticsSnapshot
import com.guanjiazhushou.assistant.domain.repository.RoomRepository
import com.guanjiazhushou.assistant.domain.repository.SettingsRepository
import com.guanjiazhushou.assistant.domain.usecase.CalculateStatisticsUseCase
import com.guanjiazhushou.assistant.presentation.navigation.getNavString
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatisticsUiState(
    val scope: StatisticsScope = StatisticsScope.MINE,
    val snapshot: StatisticsSnapshot? = null,
    val livingStatusSettings: List<LivingStatusSetting> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val calculateStatisticsUseCase: CalculateStatisticsUseCase,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics(StatisticsScope.MINE)
        viewModelScope.launch {
            settingsRepository.observeLivingStatusSettings().collect { settings ->
                _uiState.update { it.copy(livingStatusSettings = settings) }
            }
        }
    }

    fun setScope(scope: StatisticsScope) {
        if (_uiState.value.scope == scope) return
        loadStatistics(scope)
    }

    fun loadStatistics(scope: StatisticsScope) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, scope = scope) }
            val snapshot = calculateStatisticsUseCase(scope)
            _uiState.update { it.copy(snapshot = snapshot, isLoading = false) }
        }
    }
}

@HiltViewModel
class RoomListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val roomRepository: RoomRepository,
) : ViewModel() {

    private val filter = RoomListFilter.valueOf(
        checkNotNull(savedStateHandle.getNavString("filterType")),
    )
    private val scope = StatisticsScope.valueOf(
        checkNotNull(savedStateHandle.getNavString("scopeType")),
    )

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()
    val title: String = filter.title

    init {
        viewModelScope.launch {
            _rooms.value = roomRepository.getRoomsForFilter(scope, filter)
        }
    }
}
