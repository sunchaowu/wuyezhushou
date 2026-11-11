package com.guanjiazhushou.assistant.data.local.dao

import androidx.room.Dao
import androidx.room.Query

data class LivingStatusCountRow(
    val livingStatus: String,
    val count: Int,
)

data class PaymentTypeCountRow(
    val paymentType: String,
    val count: Int,
)

data class FeatureTagCountRow(
    val tagId: Long,
    val name: String,
    val iconKey: String?,
    val count: Int,
)

data class ArrearsRangeCountRow(
    val rangeLabel: String,
    val count: Int,
)

@Dao
interface StatisticsDao {
    @Query("SELECT COUNT(*) FROM rooms WHERE building_id IN (:buildingIds)")
    suspend fun countTotalRooms(buildingIds: List<Long>): Int

    @Query(
        """
        SELECT COUNT(*) FROM rooms r
        INNER JOIN living_status_settings lss ON r.living_status = lss.status_key
        WHERE lss.counts_as_occupied = 1
          AND r.building_id IN (:buildingIds)
        """,
    )
    suspend fun countOccupiedRooms(buildingIds: List<Long>): Int

    @Query(
        """
        SELECT COUNT(*) FROM rooms
        WHERE payment_type IN ('prepaid', 'normal')
          AND building_id IN (:buildingIds)
        """,
    )
    suspend fun countCollectionNumerator(buildingIds: List<Long>): Int

    @Query(
        """
        SELECT COUNT(*) FROM rooms
        WHERE living_status = 'rent_rented'
          AND building_id IN (:buildingIds)
        """,
    )
    suspend fun countRentedRooms(buildingIds: List<Long>): Int

    @Query(
        """
        SELECT COUNT(*) FROM rooms
        WHERE payment_type = 'arrears'
          AND building_id IN (:buildingIds)
        """,
    )
    suspend fun countArrearsRooms(buildingIds: List<Long>): Int

    @Query(
        """
        SELECT COUNT(*) FROM rooms
        WHERE payment_type = 'arrears'
          AND payment_months = 1
          AND building_id IN (:buildingIds)
        """,
    )
    suspend fun countArrearsOneMonth(buildingIds: List<Long>): Int

    @Query(
        """
        SELECT COUNT(*) FROM rooms
        WHERE payment_type = 'arrears'
          AND payment_months BETWEEN 2 AND 3
          AND building_id IN (:buildingIds)
        """,
    )
    suspend fun countArrearsTwoToThreeMonths(buildingIds: List<Long>): Int

    @Query(
        """
        SELECT COUNT(*) FROM rooms
        WHERE payment_type = 'arrears'
          AND payment_months BETWEEN 4 AND 6
          AND building_id IN (:buildingIds)
        """,
    )
    suspend fun countArrearsFourToSixMonths(buildingIds: List<Long>): Int

    @Query(
        """
        SELECT COUNT(*) FROM rooms
        WHERE payment_type = 'arrears'
          AND payment_months >= 7
          AND building_id IN (:buildingIds)
        """,
    )
    suspend fun countArrearsSevenPlusMonths(buildingIds: List<Long>): Int

    @Query(
        """
        SELECT r.living_status AS livingStatus, COUNT(*) AS count
        FROM rooms r
        WHERE r.building_id IN (:buildingIds)
        GROUP BY r.living_status
        ORDER BY count DESC
        """,
    )
    suspend fun getLivingStatusDistribution(buildingIds: List<Long>): List<LivingStatusCountRow>

    @Query(
        """
        SELECT r.payment_type AS paymentType, COUNT(*) AS count
        FROM rooms r
        WHERE r.building_id IN (:buildingIds)
        GROUP BY r.payment_type
        ORDER BY count DESC
        """,
    )
    suspend fun getPaymentTypeDistribution(buildingIds: List<Long>): List<PaymentTypeCountRow>

    @Query(
        """
        SELECT ft.id AS tagId, ft.name AS name, ft.icon_key AS iconKey,
               COUNT(rf.room_id) AS count
        FROM feature_tags ft
        LEFT JOIN room_features rf ON ft.id = rf.feature_tag_id
        LEFT JOIN rooms r ON rf.room_id = r.id
            AND r.building_id IN (:buildingIds)
        GROUP BY ft.id
        ORDER BY count DESC, ft.sort_order ASC
        """,
    )
    suspend fun getFeatureTagProfile(buildingIds: List<Long>): List<FeatureTagCountRow>

    @Query(
        """
        SELECT ft.id AS tagId, ft.name AS name, ft.icon_key AS iconKey,
               COUNT(rf.room_id) AS count
        FROM feature_tags ft
        LEFT JOIN room_features rf ON ft.id = rf.feature_tag_id
        LEFT JOIN rooms r ON rf.room_id = r.id AND r.building_id = :buildingId
        GROUP BY ft.id
        ORDER BY count DESC, ft.sort_order ASC
        """,
    )
    suspend fun getFeatureTagProfileForBuilding(buildingId: Long): List<FeatureTagCountRow>

    @Query("SELECT id FROM buildings WHERE source_type = 'local'")
    suspend fun getLocalBuildingIds(): List<Long>

    @Query("SELECT id FROM buildings")
    suspend fun getAllBuildingIds(): List<Long>
}
