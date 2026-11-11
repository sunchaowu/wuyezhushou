package com.guanjiazhushou.assistant.data.repository

import com.guanjiazhushou.assistant.data.local.dao.StatisticsDao
import com.guanjiazhushou.assistant.data.local.mapper.toArrearsRangeCounts
import com.guanjiazhushou.assistant.data.local.mapper.toDomain
import com.guanjiazhushou.assistant.domain.model.ArrearsRange
import com.guanjiazhushou.assistant.domain.model.BuildingMetrics
import com.guanjiazhushou.assistant.domain.model.StatisticsScope
import com.guanjiazhushou.assistant.domain.model.StatisticsSnapshot
import com.guanjiazhushou.assistant.domain.model.LivingStatus
import com.guanjiazhushou.assistant.domain.repository.SettingsRepository
import com.guanjiazhushou.assistant.domain.repository.StatisticsRepository
import com.guanjiazhushou.assistant.domain.usecase.CalculateStatisticsUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatisticsRepositoryImpl @Inject constructor(
    private val statisticsDao: StatisticsDao,
    private val settingsRepository: SettingsRepository,
) : StatisticsRepository {
    override suspend fun getBuildingIdsForScope(scope: StatisticsScope): List<Long> =
        when (scope) {
            StatisticsScope.MINE -> statisticsDao.getLocalBuildingIds()
            StatisticsScope.OVERALL -> statisticsDao.getAllBuildingIds()
        }

    override suspend fun calculateSnapshot(
        scope: StatisticsScope,
        buildingIds: List<Long>,
    ): StatisticsSnapshot {
        if (buildingIds.isEmpty()) {
            return emptySnapshot(scope, buildingIds)
        }

        val totalRooms = statisticsDao.countTotalRooms(buildingIds)
        val occupiedCount = statisticsDao.countOccupiedRooms(buildingIds)
        val collectionCount = statisticsDao.countCollectionNumerator(buildingIds)
        val rentedCount = statisticsDao.countRentedRooms(buildingIds)
        val arrearsCount = statisticsDao.countArrearsRooms(buildingIds)

        val arrearsByRange = mapOf(
            ArrearsRange.ONE_MONTH to statisticsDao.countArrearsOneMonth(buildingIds),
            ArrearsRange.TWO_TO_THREE to statisticsDao.countArrearsTwoToThreeMonths(buildingIds),
            ArrearsRange.FOUR_TO_SIX to statisticsDao.countArrearsFourToSixMonths(buildingIds),
            ArrearsRange.SEVEN_PLUS to statisticsDao.countArrearsSevenPlusMonths(buildingIds),
        ).toArrearsRangeCounts()

        val livingSettings = settingsRepository.getLivingStatusSettings()
        val labelByKey = livingSettings.associate { it.statusKey to it.label }

        return StatisticsSnapshot(
            scope = scope,
            buildingIds = buildingIds,
            totalRooms = totalRooms,
            occupiedCount = occupiedCount,
            occupancyRate = CalculateStatisticsUseCase.rate(occupiedCount, totalRooms),
            collectionCount = collectionCount,
            collectionRate = CalculateStatisticsUseCase.rate(collectionCount, totalRooms),
            rentedCount = rentedCount,
            rentalRate = CalculateStatisticsUseCase.rate(rentedCount, totalRooms),
            arrearsCount = arrearsCount,
            arrearsByRange = arrearsByRange,
            livingStatusDistribution = statisticsDao.getLivingStatusDistribution(buildingIds)
                .map { row ->
                    val label = labelByKey[row.livingStatus]
                        ?: LivingStatus.fromKey(row.livingStatus)?.label
                        ?: row.livingStatus
                    row.toDomain(label)
                },
            paymentTypeDistribution = statisticsDao.getPaymentTypeDistribution(buildingIds)
                .mapNotNull { it.toDomain() },
            featureTagProfile = statisticsDao.getFeatureTagProfile(buildingIds)
                .map { it.toDomain() },
        )
    }

    override suspend fun getBuildingMetrics(buildingId: Long): BuildingMetrics {
        val ids = listOf(buildingId)
        val total = statisticsDao.countTotalRooms(ids)
        if (total == 0) return BuildingMetrics()
        val occupied = statisticsDao.countOccupiedRooms(ids)
        val collected = statisticsDao.countCollectionNumerator(ids)
        val rented = statisticsDao.countRentedRooms(ids)
        val arrears = statisticsDao.countArrearsRooms(ids)
        return BuildingMetrics(
            totalRooms = total,
            occupancyRate = CalculateStatisticsUseCase.rate(occupied, total),
            collectionRate = CalculateStatisticsUseCase.rate(collected, total),
            rentRate = CalculateStatisticsUseCase.rate(rented, total),
            arrearsCount = arrears,
        )
    }

    override suspend fun getAggregateMetrics(buildingIds: List<Long>): BuildingMetrics {
        if (buildingIds.isEmpty()) return BuildingMetrics()
        val total = statisticsDao.countTotalRooms(buildingIds)
        if (total == 0) return BuildingMetrics()
        val occupied = statisticsDao.countOccupiedRooms(buildingIds)
        val collected = statisticsDao.countCollectionNumerator(buildingIds)
        val rented = statisticsDao.countRentedRooms(buildingIds)
        val arrears = statisticsDao.countArrearsRooms(buildingIds)
        return BuildingMetrics(
            totalRooms = total,
            occupancyRate = CalculateStatisticsUseCase.rate(occupied, total),
            collectionRate = CalculateStatisticsUseCase.rate(collected, total),
            rentRate = CalculateStatisticsUseCase.rate(rented, total),
            arrearsCount = arrears,
        )
    }

    private fun emptySnapshot(scope: StatisticsScope, buildingIds: List<Long>): StatisticsSnapshot =
        StatisticsSnapshot(
            scope = scope,
            buildingIds = buildingIds,
            totalRooms = 0,
            occupiedCount = 0,
            occupancyRate = 0f,
            collectionCount = 0,
            collectionRate = 0f,
            rentedCount = 0,
            rentalRate = 0f,
            arrearsCount = 0,
            arrearsByRange = emptyMap<ArrearsRange, Int>().toArrearsRangeCounts(),
            livingStatusDistribution = emptyList(),
            paymentTypeDistribution = emptyList(),
            featureTagProfile = emptyList(),
        )
}
