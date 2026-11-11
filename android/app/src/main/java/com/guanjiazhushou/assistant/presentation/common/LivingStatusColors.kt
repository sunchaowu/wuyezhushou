package com.guanjiazhushou.assistant.presentation.common

import androidx.compose.ui.graphics.Color
import com.guanjiazhushou.assistant.domain.model.LivingStatus
import com.guanjiazhushou.assistant.domain.model.LivingStatusColorDefaults
import com.guanjiazhushou.assistant.domain.model.LivingStatusSetting
import com.guanjiazhushou.assistant.ui.theme.AppThemePalette

fun List<LivingStatusSetting>.toStatusColorMap(): Map<String, Color> =
    associate { it.statusKey to Color(it.colorArgb.toInt()) }

fun List<LivingStatusSetting>.toStatusLabelMap(): Map<String, String> =
    associate { it.statusKey to it.label }

fun Map<String, Color>.livingStatusColor(statusKey: String, fallback: Color = Color(LivingStatusColorDefaults.forKey(statusKey).toInt())): Color =
    this[statusKey] ?: fallback

fun Map<String, String>.livingStatusLabel(statusKey: String): String =
    this[statusKey] ?: LivingStatus.resolveLabel(statusKey)

/** Room cell background: living status color with readable opacity. */
fun Color.asRoomCellBackground(alpha: Float = 0.72f): Color = copy(alpha = alpha)

/**
 * Statistics / charts: living status colors follow the active theme's mono-hue ladder
 * ([AppThemePalette.livingColors] + [AppThemePalette.chartColors]), not fixed gray defaults.
 */
fun AppThemePalette.livingStatusColor(
    statusKey: String,
    settings: List<LivingStatusSetting> = emptyList(),
): Color {
    LivingStatus.fromKey(statusKey)?.let { status ->
        livingColors[status]?.let { return it }
    }
    val orderIndex = settings.indexOfFirst { it.statusKey == statusKey }.takeIf { it >= 0 }
        ?: LivingStatus.entries.indexOfFirst { it.key == statusKey }
    return if (orderIndex >= 0) {
        chartColors[orderIndex % chartColors.size]
    } else {
        primary
    }
}

fun AppThemePalette.livingStatusColorMap(
    settings: List<LivingStatusSetting>,
): Map<String, Color> {
    val keys = if (settings.isNotEmpty()) {
        settings.map { it.statusKey }
    } else {
        LivingStatus.entries.map { it.key }
    }
    return keys.associateWith { livingStatusColor(it, settings) }
}
