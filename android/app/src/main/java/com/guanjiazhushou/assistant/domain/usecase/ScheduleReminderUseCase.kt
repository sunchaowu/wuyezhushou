package com.guanjiazhushou.assistant.domain.usecase

import com.guanjiazhushou.assistant.data.reminder.ReminderScheduler
import com.guanjiazhushou.assistant.domain.model.ReminderItem
import com.guanjiazhushou.assistant.domain.model.ReminderStatus
import com.guanjiazhushou.assistant.domain.model.ReminderTargetType
import com.guanjiazhushou.assistant.domain.repository.ReminderRepository
import javax.inject.Inject

class ScheduleReminderUseCase @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
) {
    suspend operator fun invoke(
        targetType: ReminderTargetType,
        targetId: Long,
        buildingId: Long,
        roomId: Long,
        title: String,
        body: String,
        triggerAt: Long,
    ): Long {
        require(triggerAt > System.currentTimeMillis()) {
            "提醒时间必须晚于当前时间"
        }
        reminderRepository.cancelForTarget(targetType, targetId)
        reminderScheduler.cancelForTarget(targetType, targetId)
        val id = reminderRepository.insert(
            ReminderItem(
                targetType = targetType,
                targetId = targetId,
                buildingId = buildingId,
                roomId = roomId,
                title = title.trim(),
                body = body.trim(),
                triggerAt = triggerAt,
                status = ReminderStatus.SCHEDULED,
                createdAt = System.currentTimeMillis(),
            ),
        )
        val item = checkNotNull(reminderRepository.getById(id))
        reminderScheduler.schedule(item)
        return id
    }
}
