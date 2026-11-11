package com.guanjiazhushou.assistant.presentation.common

import java.util.Locale

/** Formats a percentage value (0–100 scale) with two decimal places, e.g. `12.34%`. */
fun formatPercent(rate: Float): String =
    String.format(Locale.US, "%.2f%%", rate)

/** Formats a ratio (0–1 scale) as percentage with two decimal places. */
fun formatRatioPercent(ratio: Float): String =
    formatPercent(ratio * 100f)
