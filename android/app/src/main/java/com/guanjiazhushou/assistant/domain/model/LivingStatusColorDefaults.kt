package com.guanjiazhushou.assistant.domain.model

object LivingStatusColorDefaults {
    private val builtinColors = mapOf(
        LivingStatus.SELF_LONG_TERM.key to 0xFF1D4ED8L,
        LivingStatus.SELF_VACATION.key to 0xFF38BDF8L,
        LivingStatus.RENT_RENTED.key to 0xFF2563EBL,
        LivingStatus.RENT_AVAILABLE.key to 0xFF60A5FAL,
        LivingStatus.VACANT_DELIVERED.key to 0xFF94A3B8L,
        LivingStatus.VACANT_UNDELIVERED.key to 0xFFCBD5E1L,
    )

    val presetOptions: List<Long> = listOf(
        0xFF1D4ED8L,
        0xFF2563EBL,
        0xFF38BDF8L,
        0xFF059669L,
        0xFF14B8A6L,
        0xFFF59E0BL,
        0xFFEF4444L,
        0xFF8B5CF6L,
        0xFFEC4899L,
        0xFF64748BL,
        0xFF94A3B8L,
        0xFFCBD5E1L,
    )

    fun forKey(statusKey: String): Long = builtinColors[statusKey] ?: presetOptions.first()
}
