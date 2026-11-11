package com.guanjiazhushou.assistant.presentation.building

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guanjiazhushou.assistant.domain.model.Building
import com.guanjiazhushou.assistant.domain.model.BuildingMetrics
import com.guanjiazhushou.assistant.domain.model.CommunityBuildingGroup
import com.guanjiazhushou.assistant.domain.repository.BuildingRepository
import com.guanjiazhushou.assistant.domain.repository.CommunityRepository
import com.guanjiazhushou.assistant.domain.repository.StatisticsRepository
import com.guanjiazhushou.assistant.domain.usecase.DeleteBuildingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class BuildingListUiState(
    val buildingGroups: List<CommunityBuildingGroup> = emptyList(),
    val metricsByBuildingId: Map<Long, BuildingMetrics> = emptyMap(),
    val summary: BuildingMetrics = BuildingMetrics(),
) {
    val buildings: List<Building> = buildingGroups.flatMap { it.buildings }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BuildingListViewModel @Inject constructor(
    private val buildingRepository: BuildingRepository,
    private val communityRepository: CommunityRepository,
    private val statisticsRepository: StatisticsRepository,
    private val deleteBuildingUseCase: DeleteBuildingUseCase,
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val uiState: StateFlow<BuildingListUiState> = combine(
        buildingRepository.observeAll(),
        communityRepository.observeAll(),
    ) { buildings, communities -> buildings to communities }
        .mapLatest { (buildings, communities) ->
            val communityNameById = communities.associate { it.id to it.name }
            val buildingGroups = buildings
                .groupBy { it.communityId }
                .map { (communityId, groupBuildings) ->
                    CommunityBuildingGroup(
                        communityId = communityId,
                        communityName = communityNameById[communityId]?.takeIf { it.isNotBlank() }
                            ?: "未命名小区",
                        buildings = groupBuildings.sortedBy { it.name },
                    )
                }
                .sortedBy { it.communityName }
            val metricsMap = withContext(Dispatchers.IO) {
                buildings.associate { building ->
                    building.id to statisticsRepository.getBuildingMetrics(building.id)
                }
            }
            val summary = withContext(Dispatchers.IO) {
                statisticsRepository.getAggregateMetrics(buildings.map { it.id })
            }
            BuildingListUiState(
                buildingGroups = buildingGroups,
                metricsByBuildingId = metricsMap,
                summary = summary,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BuildingListUiState(),
        )

    fun deleteBuilding(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                deleteBuildingUseCase(id)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "删除楼栋失败"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
