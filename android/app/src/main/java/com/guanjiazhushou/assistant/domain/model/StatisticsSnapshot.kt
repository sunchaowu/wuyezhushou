package com.guanjiazhushou.assistant.domain.model

data class LivingStatusCount(
    val statusKey: String,
    val label: String,
    val count: Int,
)

data class PaymentTypeCount(
    val type: PaymentType,
    val count: Int,
)

data class FeatureTagCount(
    val tagId: Long,
    val name: String,
    val iconKey: String?,
    val count: Int,
)

data class ArrearsRangeCount(
    val range: ArrearsRange,
    val count: Int,
)

data class StatisticsSnapshot(
    val scope: StatisticsScope,
    val buildingIds: List<Long>,
    val totalRooms: Int,
    val occupiedCount: Int,
    val occupancyRate: Float,
    val collectionCount: Int,
    val collectionRate: Float,
    val rentedCount: Int,
    val rentalRate: Float,
    val arrearsCount: Int,
    val arrearsByRange: List<ArrearsRangeCount>,
    val livingStatusDistribution: List<LivingStatusCount>,
    val paymentTypeDistribution: List<PaymentTypeCount>,
    val featureTagProfile: List<FeatureTagCount>,
)
