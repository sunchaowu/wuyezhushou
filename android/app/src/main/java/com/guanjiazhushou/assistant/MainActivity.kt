package com.guanjiazhushou.assistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.guanjiazhushou.assistant.data.reminder.ReminderDeepLink
import com.guanjiazhushou.assistant.data.reminder.ReminderIntentExtras
import com.guanjiazhushou.assistant.data.reminder.ReminderNavigationBus
import com.guanjiazhushou.assistant.data.reminder.ReminderReceiver
import com.guanjiazhushou.assistant.domain.model.ReminderTargetType
import com.guanjiazhushou.assistant.presentation.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var reminderNavigationBus: ReminderNavigationBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        offerReminderDeepLink(intent)
        setContent {
            val pendingDeepLink by reminderNavigationBus.pending.collectAsState()
            Surface(modifier = Modifier.fillMaxSize()) {
                AppNavigation(
                    reminderDeepLink = pendingDeepLink,
                    onReminderDeepLinkConsumed = reminderNavigationBus::consume,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        offerReminderDeepLink(intent)
    }

    private fun offerReminderDeepLink(intent: Intent?) {
        if (intent == null) return
        val isReminder = intent.action == ReminderReceiver.ACTION_OPEN_REMINDER ||
            intent.hasExtra(ReminderIntentExtras.EXTRA_ROOM_ID)
        if (!isReminder) return
        val roomId = intent.getLongExtra(ReminderIntentExtras.EXTRA_ROOM_ID, -1L)
        if (roomId < 0) return
        val buildingId = intent.getLongExtra(ReminderIntentExtras.EXTRA_BUILDING_ID, 0L)
        val targetType = ReminderTargetType.fromKey(
            intent.getStringExtra(ReminderIntentExtras.EXTRA_TARGET_TYPE).orEmpty(),
        )
        val targetId = intent.getLongExtra(ReminderIntentExtras.EXTRA_TARGET_ID, roomId)
        reminderNavigationBus.offer(
            ReminderDeepLink(
                buildingId = buildingId,
                roomId = roomId,
                targetType = targetType,
                targetId = targetId,
            ),
        )
    }
}
