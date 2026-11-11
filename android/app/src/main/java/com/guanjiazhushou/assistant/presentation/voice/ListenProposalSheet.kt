package com.guanjiazhushou.assistant.presentation.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guanjiazhushou.assistant.domain.model.PaymentType
import com.guanjiazhushou.assistant.domain.voice.CandidateAction
import com.guanjiazhushou.assistant.domain.voice.ListenIntent

@Composable
fun ListenTextSimDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("模拟口述（开发/验收）") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("口述文本") },
                placeholder = { Text("如：601欠费三个月") },
                minLines = 2,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSubmit(text)
                    onDismiss()
                },
                enabled = text.isNotBlank(),
            ) { Text("解析") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
fun ListenProposalSheet(
    proposals: List<CandidateAction>,
    intent: ListenIntent? = null,
    onConfirmPayment: (CandidateAction.ProposePayment, PaymentType, Int) -> Unit,
    onConfirmEvent: (CandidateAction.ProposeEvent) -> Unit,
    onConfirmTags: (CandidateAction.ProposeFeatureTags) -> Unit,
    onDismiss: () -> Unit,
    onSchedulePaymentFollowUp: ((CandidateAction.ProposePayment, PaymentType, Int, Long) -> Unit)? = null,
) {
    if (proposals.isEmpty()) return
    val payment = proposals.filterIsInstance<CandidateAction.ProposePayment>().firstOrNull()
    val event = proposals.filterIsInstance<CandidateAction.ProposeEvent>().firstOrNull()
    val tags = proposals.filterIsInstance<CandidateAction.ProposeFeatureTags>().firstOrNull()

    var monthsText by remember(payment) {
        mutableStateOf((payment?.months ?: 1).toString())
    }
    var selectedType by remember(payment) {
        mutableStateOf(payment?.paymentType ?: PaymentType.ARREARS)
    }
    var alsoRemind by remember(payment) { mutableStateOf(false) }
    var showRemindPicker by remember { mutableStateOf(false) }

    if (showRemindPicker && payment != null && onSchedulePaymentFollowUp != null) {
        com.guanjiazhushou.assistant.presentation.reminder.ScheduleReminderDialog(
            title = "缴费跟进提醒",
            subtitle = "写入后顺便设置催缴提醒",
            onDismiss = {
                showRemindPicker = false
                onDismiss()
            },
            onConfirm = { triggerAt ->
                val months = monthsText.toIntOrNull()?.coerceAtLeast(0) ?: 1
                onConfirmPayment(payment, selectedType, months)
                onSchedulePaymentFollowUp(payment, selectedType, months, triggerAt)
                showRemindPicker = false
                onDismiss()
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("听记提案确认")
                if (intent != null) {
                    val pct = (intent.confidence * 100f).toInt().coerceIn(0, 100)
                    Text("意图：${intent.displayName} · ${pct}%")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                payment?.let { p ->
                    Text("收费：房${p.roomNumber ?: "当前房"} / ${p.rawText}")
                    if (p.needsAmountClarification) {
                        Text("检测到金额 ${p.amountYuan} 元，系统以「月」计量，请确认月数或仅忽略。")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PaymentType.entries.forEach { type ->
                            OutlinedButton(onClick = { selectedType = type }) {
                                Text(
                                    if (selectedType == type) "✓ ${type.label}" else type.label,
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = monthsText,
                        onValueChange = { monthsText = it.filter(Char::isDigit).take(3) },
                        label = { Text("月数") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (onSchedulePaymentFollowUp != null) {
                        OutlinedButton(onClick = { alsoRemind = !alsoRemind }) {
                            Text(if (alsoRemind) "✓ 顺便设催缴提醒" else "顺便设催缴提醒")
                        }
                    }
                }
                event?.let { e ->
                    Text("事件：房${e.roomNumber ?: "当前房"} / ${e.eventType} / ${e.summary}")
                }
                tags?.let { t ->
                    val action = if (t.remove) "移除" else "添加"
                    Text("特征标签：房${t.roomNumber ?: "当前房"} $action ${t.tagIds}")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (payment != null && alsoRemind && onSchedulePaymentFollowUp != null) {
                        showRemindPicker = true
                        return@Button
                    }
                    payment?.let {
                        val months = monthsText.toIntOrNull()?.coerceAtLeast(0) ?: 1
                        onConfirmPayment(it, selectedType, months)
                    }
                    event?.let(onConfirmEvent)
                    tags?.let(onConfirmTags)
                    onDismiss()
                },
            ) { Text("确认写入") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("忽略") }
        },
    )
}

@Composable
fun RoomDisambiguationDialog(
    roomNumbers: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择房屋") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("匹配到多个房号，请选择：")
                roomNumbers.forEach { number ->
                    OutlinedButton(
                        onClick = { onSelect(number) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(number) }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}
