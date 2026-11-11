package com.guanjiazhushou.assistant.domain.model

enum class ReminderTargetType(val key: String) {
    EVENT("event"),
    PAYMENT_FOLLOWUP("payment_followup"),
    ;

    companion object {
        fun fromKey(key: String): ReminderTargetType =
            entries.firstOrNull { it.key == key } ?: EVENT
    }
}

enum class ReminderStatus(val key: String) {
    SCHEDULED("scheduled"),
    FIRED("fired"),
    CANCELLED("cancelled"),
    ;

    companion object {
        fun fromKey(key: String): ReminderStatus =
            entries.firstOrNull { it.key == key } ?: SCHEDULED
    }
}

data class ReminderItem(
    val id: Long = 0,
    val targetType: ReminderTargetType,
    val targetId: Long,
    val buildingId: Long,
    val roomId: Long,
    val title: String,
    val body: String,
    val triggerAt: Long,
    val status: ReminderStatus = ReminderStatus.SCHEDULED,
    val createdAt: Long,
)
