package com.guanjiazhushou.assistant.domain.model

data class LivingStatusSetting(
    val statusKey: String,
    val label: String,
    val colorArgb: Long,
    val countsAsOccupied: Boolean,
    val isBuiltin: Boolean,
    val sortOrder: Int,
)
