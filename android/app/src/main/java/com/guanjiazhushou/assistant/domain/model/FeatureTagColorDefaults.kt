package com.guanjiazhushou.assistant.domain.model

object FeatureTagColorDefaults {
    private val builtinColors = mapOf(
        "elderly" to 0xFF7C3AEDL,
        "child" to 0xFF0EA5E9L,
        "pet" to 0xFFF59E0BL,
        "for_sale" to 0xFFEF4444L,
        "vehicle" to 0xFF059669L,
    )

    fun forTag(tag: FeatureTag): Long {
        if (tag.iconType == IconType.BUILTIN && !tag.iconKey.isNullOrBlank()) {
            builtinColors[tag.iconKey]?.let { return it }
        }
        return LivingStatusColorDefaults.presetOptions[
            (tag.sortOrder.coerceAtLeast(0)) % LivingStatusColorDefaults.presetOptions.size
        ]
    }
}
