package com.guanjiazhushou.assistant.data.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.guanjiazhushou.assistant.MainActivity
import com.guanjiazhushou.assistant.R
import com.guanjiazhushou.assistant.domain.repository.ReminderRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var reminderRepository: ReminderRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val reminderId = intent.getLongExtra(ReminderIntentExtras.EXTRA_REMINDER_ID, -1L)
        if (reminderId < 0) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val item = reminderRepository.getById(reminderId) ?: return@launch
                reminderRepository.markFired(reminderId)
                ensureChannel(context)
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    action = ACTION_OPEN_REMINDER
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(ReminderIntentExtras.EXTRA_REMINDER_ID, item.id)
                    putExtra(ReminderIntentExtras.EXTRA_BUILDING_ID, item.buildingId)
                    putExtra(ReminderIntentExtras.EXTRA_ROOM_ID, item.roomId)
                    putExtra(ReminderIntentExtras.EXTRA_TARGET_TYPE, item.targetType.key)
                    putExtra(ReminderIntentExtras.EXTRA_TARGET_ID, item.targetId)
                }
                val contentPending = PendingIntent.getActivity(
                    context,
                    reminderId.toInt(),
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(item.title)
                    .setContentText(item.body)
                    .setContentIntent(contentPending)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()
                NotificationManagerCompat.from(context).notify(reminderId.toInt(), notification)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "跟进提醒",
            NotificationManager.IMPORTANCE_HIGH,
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_FIRE = "com.guanjiazhushou.assistant.action.REMINDER_FIRE"
        const val ACTION_OPEN_REMINDER = "com.guanjiazhushou.assistant.action.OPEN_REMINDER"
        const val CHANNEL_ID = "followups"

        @Deprecated("Use ReminderIntentExtras", ReplaceWith("ReminderIntentExtras.EXTRA_REMINDER_ID"))
        const val EXTRA_REMINDER_ID = ReminderIntentExtras.EXTRA_REMINDER_ID
    }
}
