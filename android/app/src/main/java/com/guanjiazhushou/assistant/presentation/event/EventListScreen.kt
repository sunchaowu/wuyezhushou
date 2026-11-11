package com.guanjiazhushou.assistant.presentation.event

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guanjiazhushou.assistant.domain.model.RoomEvent
import com.guanjiazhushou.assistant.domain.model.RoomEventStatus
import com.guanjiazhushou.assistant.domain.model.RoomEventType
import com.guanjiazhushou.assistant.presentation.reminder.ReminderPermission
import com.guanjiazhushou.assistant.presentation.reminder.ScheduleReminderDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    onBack: () -> Unit,
    viewModel: EventListViewModel = hiltViewModel(),
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCreate by remember { mutableStateOf(false) }
    var remindEvent by remember { mutableStateOf<RoomEvent?>(null) }
    var notificationAsked by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* schedule path surfaces exact-alarm errors if still blocked */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !ReminderPermission.hasPostNotifications(context) &&
            !notificationAsked
        ) {
            notificationAsked = true
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("操作失败") },
            text = { Text(errorMessage!!) },
            confirmButton = {
                TextButton(onClick = viewModel::clearError) { Text("知道了") }
            },
        )
    }

    if (showCreate) {
        ManualEventDialog(
            onDismiss = { showCreate = false },
            onConfirm = { buildingId, roomId, type, summary ->
                viewModel.createManual(buildingId, roomId, type, summary)
                showCreate = false
            },
        )
    }

    remindEvent?.let { event ->
        ScheduleReminderDialog(
            title = "事件提醒",
            subtitle = event.summary,
            onDismiss = { remindEvent = null },
            onConfirm = { triggerAt ->
                if (!ReminderPermission.hasPostNotifications(context) &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (!ReminderPermission.canScheduleExactAlarms(context)) {
                    context.startActivity(ReminderPermission.exactAlarmSettingsIntent(context))
                    remindEvent = null
                    return@ScheduleReminderDialog
                }
                viewModel.scheduleReminder(event, triggerAt)
                remindEvent = null
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("业务事件") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = "手动创建事件")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            StatusFilterRow(
                selected = statusFilter,
                onSelect = viewModel::setStatusFilter,
            )
            if (events.isEmpty()) {
                Text(
                    text = "暂无事件。可点右下角手动创建，或在楼栋网格听记中口述报修/扰民等内容。",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(events, key = { it.id }) { event ->
                        EventCard(
                            event = event,
                            onMarkDone = {
                                viewModel.setStatus(event, RoomEventStatus.DONE)
                            },
                            onMarkInProgress = {
                                viewModel.setStatus(event, RoomEventStatus.IN_PROGRESS)
                            },
                            onCancel = {
                                viewModel.setStatus(event, RoomEventStatus.CANCELLED)
                            },
                            onRemind = { remindEvent = event },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusFilterRow(
    selected: RoomEventStatus?,
    onSelect: (RoomEventStatus?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text("全部") },
        )
        RoomEventStatus.entries.forEach { status ->
            FilterChip(
                selected = selected == status,
                onClick = { onSelect(status) },
                label = { Text(status.label) },
            )
        }
    }
}

@Composable
private fun ManualEventDialog(
    onDismiss: () -> Unit,
    onConfirm: (buildingId: Long, roomId: Long, type: RoomEventType, summary: String) -> Unit,
) {
    var buildingIdText by remember { mutableStateOf("") }
    var roomIdText by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(RoomEventType.REPAIR) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("手动创建事件") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = buildingIdText,
                    onValueChange = { buildingIdText = it.filter(Char::isDigit) },
                    label = { Text("楼栋 ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = roomIdText,
                    onValueChange = { roomIdText = it.filter(Char::isDigit) },
                    label = { Text("房屋 ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoomEventType.entries.take(3).forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = { Text(option.label) },
                        )
                    }
                }
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("摘要") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        buildingIdText.toLongOrNull() ?: 0L,
                        roomIdText.toLongOrNull() ?: 0L,
                        type,
                        summary,
                    )
                },
                enabled = summary.isNotBlank(),
            ) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun EventCard(
    event: RoomEvent,
    onMarkDone: () -> Unit,
    onMarkInProgress: () -> Unit,
    onCancel: () -> Unit,
    onRemind: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "${event.type.label} · ${event.status.label}",
                style = MaterialTheme.typography.titleSmall,
            )
            Text("楼栋 ${event.buildingId} / 房 ${event.roomId}")
            Text(event.summary)
            event.rawText?.let {
                Text("原声：$it", style = MaterialTheme.typography.bodySmall)
            }
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                if (event.status == RoomEventStatus.PENDING) {
                    TextButton(onClick = onMarkInProgress) { Text("处理中") }
                }
                if (event.status == RoomEventStatus.PENDING ||
                    event.status == RoomEventStatus.IN_PROGRESS
                ) {
                    TextButton(onClick = onMarkDone) { Text("标记完成") }
                    TextButton(onClick = onRemind) { Text("设提醒") }
                    TextButton(onClick = onCancel) { Text("取消") }
                }
            }
        }
    }
}
