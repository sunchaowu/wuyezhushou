package com.guanjiazhushou.assistant.domain.repository

import com.guanjiazhushou.assistant.domain.model.BuildingMetrics
import com.guanjiazhushou.assistant.domain.model.StatisticsScope
import com.guanjiazhushou.assistant.domain.model.StatisticsSnapshot

interface StatisticsRepository {
    suspend fun getBuildingIdsForScope(scope: StatisticsScope): List<Long>
    suspend fun calculateSnapshot(scope: StatisticsScope, buildingIds: List<Long>): StatisticsSnapshot
    suspend fun getBuildingMetrics(buildingId: Long): BuildingMetrics
    suspend fun getAggregateMetrics(buildingIds: List<Long>): BuildingMetrics
}
