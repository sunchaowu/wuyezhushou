package com.guanjiazhushou.assistant.domain.usecase

import com.guanjiazhushou.assistant.domain.model.StatisticsScope
import com.guanjiazhushou.assistant.domain.model.StatisticsSnapshot
import com.guanjiazhushou.assistant.domain.repository.StatisticsRepository
import javax.inject.Inject

class CalculateStatisticsUseCase @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
) {
    suspend operator fun invoke(
        scope: StatisticsScope,
        buildingIds: List<Long>? = null,
    ): StatisticsSnapshot {
        val resolvedBuildingIds = buildingIds
            ?: statisticsRepository.getBuildingIdsForScope(scope)
        return statisticsRepository.calculateSnapshot(scope, resolvedBuildingIds)
    }

    companion object {
        fun rate(numerator: Int, denominator: Int): Float =
            if (denominator == 0) 0f else (numerator.toFloat() / denominator) * 100f
    }
}
