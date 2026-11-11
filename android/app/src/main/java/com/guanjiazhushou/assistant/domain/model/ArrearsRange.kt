package com.guanjiazhushou.assistant.domain.model

enum class ArrearsRange(val label: String, val minMonths: Int, val maxMonths: Int?) {
    ONE_MONTH("欠费 1 个月", 1, 1),
    TWO_TO_THREE("欠费 2-3 个月", 2, 3),
    FOUR_TO_SIX("欠费 4-6 个月", 4, 6),
    SEVEN_PLUS("欠费 7 个月以上", 7, null);
}
