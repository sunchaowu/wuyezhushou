package com.guanjiazhushou.assistant.presentation.reminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ScheduleReminderDialog(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    onConfirm: (triggerAtMs: Long) -> Unit,
) {
    val now = System.currentTimeMillis()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(subtitle)
                OutlinedButton(
                    onClick = { onConfirm(now + 60 * 1000L) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("1 分钟后") }
                OutlinedButton(
                    onClick = { onConfirm(now + 5 * 60 * 1000L) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("5 分钟后") }
                OutlinedButton(
                    onClick = { onConfirm(now + 60 * 60 * 1000L) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("1 小时后") }
                OutlinedButton(
                    onClick = { onConfirm(now + 24 * 60 * 60 * 1000L) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("明天同一时刻") }
                OutlinedButton(
                    onClick = { onConfirm(now + 3 * 24 * 60 * 60 * 1000L) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("3 天后") }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
