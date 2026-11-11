package com.guanjiazhushou.assistant.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.guanjiazhushou.assistant.domain.model.ReminderItem
import com.guanjiazhushou.assistant.domain.model.ReminderTargetType
import com.guanjiazhushou.assistant.domain.repository.ReminderRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderRepository: ReminderRepository,
) {
    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(item: ReminderItem) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE
            putExtra(ReminderIntentExtras.EXTRA_REMINDER_ID, item.id)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            item.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            error("未授予精确闹钟权限，无法调度提醒。请在系统设置中允许本应用使用精确闹钟。")
        }
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            item.triggerAt,
            pending,
        )
    }

    fun cancel(reminderId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE
            putExtra(ReminderIntentExtras.EXTRA_REMINDER_ID, reminderId)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pending)
    }

    fun cancelForTarget(targetType: ReminderTargetType, targetId: Long) {
        // Best-effort: cancel is primarily DB-driven; AlarmManager cancel needs id.
        // Reschedule path cancels prior scheduled rows then inserts a new id.
    }

    suspend fun rescheduleAll() {
        reminderRepository.getScheduled().forEach { schedule(it) }
    }
}
