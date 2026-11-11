package com.guanjiazhushou.assistant.data.local.mapper

import com.guanjiazhushou.assistant.data.local.dao.FeatureTagCountRow
import com.guanjiazhushou.assistant.data.local.dao.LivingStatusCountRow
import com.guanjiazhushou.assistant.data.local.dao.PaymentTypeCountRow
import com.guanjiazhushou.assistant.domain.model.ArrearsRange
import com.guanjiazhushou.assistant.domain.model.ArrearsRangeCount
import com.guanjiazhushou.assistant.domain.model.FeatureTagCount
import com.guanjiazhushou.assistant.domain.model.LivingStatusCount
import com.guanjiazhushou.assistant.domain.model.PaymentType
import com.guanjiazhushou.assistant.domain.model.PaymentTypeCount

fun LivingStatusCountRow.toDomain(label: String): LivingStatusCount = LivingStatusCount(
    statusKey = livingStatus,
    label = label,
    count = count,
)

fun PaymentTypeCountRow.toDomain(): PaymentTypeCount? {
    val type = PaymentType.fromKey(paymentType) ?: return null
    return PaymentTypeCount(type = type, count = count)
}

fun FeatureTagCountRow.toDomain(): FeatureTagCount = FeatureTagCount(
    tagId = tagId,
    name = name,
    iconKey = iconKey,
    count = count,
)

fun Map<ArrearsRange, Int>.toArrearsRangeCounts(): List<ArrearsRangeCount> =
    ArrearsRange.entries.map { range ->
        ArrearsRangeCount(range = range, count = this[range] ?: 0)
    }
