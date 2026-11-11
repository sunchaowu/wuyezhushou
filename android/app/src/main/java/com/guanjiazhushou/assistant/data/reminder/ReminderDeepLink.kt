package com.guanjiazhushou.assistant.data.reminder

import com.guanjiazhushou.assistant.domain.model.ReminderTargetType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class ReminderDeepLink(
    val buildingId: Long,
    val roomId: Long,
    val targetType: ReminderTargetType,
    val targetId: Long,
)

@Singleton
class ReminderNavigationBus @Inject constructor() {
    private val _pending = MutableStateFlow<ReminderDeepLink?>(null)
    val pending: StateFlow<ReminderDeepLink?> = _pending.asStateFlow()

    fun offer(link: ReminderDeepLink) {
        _pending.value = link
    }

    fun consume() {
        _pending.value = null
    }
}

object ReminderIntentExtras {
    const val EXTRA_BUILDING_ID = "reminder_building_id"
    const val EXTRA_ROOM_ID = "reminder_room_id"
    const val EXTRA_TARGET_TYPE = "reminder_target_type"
    const val EXTRA_TARGET_ID = "reminder_target_id"
    const val EXTRA_REMINDER_ID = "reminder_id"
}
