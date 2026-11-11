package com.guanjiazhushou.assistant.presentation.building

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.guanjiazhushou.assistant.domain.model.FeatureTag
import com.guanjiazhushou.assistant.presentation.common.ChipFlowRow
import com.guanjiazhushou.assistant.presentation.common.ColoredFilterChip
import com.guanjiazhushou.assistant.presentation.common.FeatureTagSmallIcon
import com.guanjiazhushou.assistant.presentation.common.displayColor
import com.guanjiazhushou.assistant.presentation.common.livingStatusColor
import com.guanjiazhushou.assistant.domain.model.LivingStatusSetting
import com.guanjiazhushou.assistant.domain.model.PaymentType
import com.guanjiazhushou.assistant.domain.model.Room

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailSheet(
    room: Room,
    livingStatusSettings: List<LivingStatusSetting>,
    featureTags: Map<Long, FeatureTag>,
    selectedFeatureTagIds: List<Long>,
    isReadonly: Boolean,
    allowDelete: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (Room, List<Long>) -> Unit,
    onDelete: () -> Unit = {},
    onToggleFeatureTag: (Long) -> Unit,
    onSchedulePaymentFollowUp: ((triggerAtMs: Long) -> Unit)? = null,
) {
    var roomNumber by remember(room.id) { mutableStateOf(room.roomNumber) }
    var paymentType by remember(room.id) { mutableStateOf(room.paymentType) }
    var paymentMonths by remember(room.id) { mutableIntStateOf(room.paymentMonths) }
    var livingStatusKey by remember(room.id) { mutableStateOf(room.livingStatusKey) }
    var ownerFeedback by remember(room.id) { mutableStateOf(room.ownerFeedback) }
    var otherNotes by remember(room.id) { mutableStateOf(room.otherNotes) }
    var showDeleteConfirm by remember(room.id) { mutableStateOf(false) }
    var showPaymentReminder by remember(room.id) { mutableStateOf(false) }

    if (showPaymentReminder && onSchedulePaymentFollowUp != null) {
        com.guanjiazhushou.assistant.presentation.reminder.ScheduleReminderDialog(
            title = "缴费跟进提醒",
            subtitle = "为 ${room.roomNumber}（${paymentType.label} ${paymentMonths}月）设置本机催缴提醒",
            onDismiss = { showPaymentReminder = false },
            onConfirm = { triggerAt ->
                showPaymentReminder = false
                onSchedulePaymentFollowUp(triggerAt)
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除铺位") },
            text = { Text("确定删除铺位 ${room.roomNumber}？相关沟通记录将一并删除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            },
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {},
                    ),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "房屋详情",
                        style = MaterialTheme.typography.titleLarge,
                    )

                    OutlinedTextField(
                        value = roomNumber,
                        onValueChange = { if (!isReadonly) roomNumber = it },
                        label = { Text("房间号") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = isReadonly,
                        singleLine = true,
                    )

                    Text(text = "缴费状态", style = MaterialTheme.typography.bodySmall)
                    ChipFlowRow(modifier = Modifier.fillMaxWidth()) {
                        PaymentType.entries.forEach { type ->
                            val paymentColor = paymentTypeColor(type)
                            ColoredFilterChip(
                                selected = paymentType == type,
                                onClick = { if (!isReadonly) paymentType = type },
                                label = type.label,
                                color = paymentColor,
                                enabled = !isReadonly,
                            )
                        }
                    }

                    OutlinedTextField(
                        value = paymentMonths.toString(),
                        onValueChange = { value ->
                            if (!isReadonly) paymentMonths = value.toIntOrNull() ?: paymentMonths
                        },
                        label = { Text("月数") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = isReadonly,
                    )

                    if (!isReadonly && onSchedulePaymentFollowUp != null) {
                        OutlinedButton(
                            onClick = { showPaymentReminder = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("设缴费跟进提醒")
                        }
                    }

                    val statusColorMap = remember(livingStatusSettings) {
                        livingStatusSettings.associate { it.statusKey to Color(it.colorArgb.toInt()) }
                    }
                    Text(text = "居住状态", style = MaterialTheme.typography.bodySmall)
                    ChipFlowRow(modifier = Modifier.fillMaxWidth()) {
                        livingStatusSettings.forEach { status ->
                            val statusColor = statusColorMap.livingStatusColor(status.statusKey)
                            ColoredFilterChip(
                                selected = livingStatusKey == status.statusKey,
                                onClick = { if (!isReadonly) livingStatusKey = status.statusKey },
                                label = status.label,
                                color = statusColor,
                                enabled = !isReadonly,
                            )
                        }
                    }

                    OutlinedTextField(
                        value = ownerFeedback,
                        onValueChange = { if (!isReadonly) ownerFeedback = it },
                        label = { Text("业主原声") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = isReadonly,
                        minLines = 2,
                        maxLines = 6,
                    )
                    OutlinedTextField(
                        value = otherNotes,
                        onValueChange = { if (!isReadonly) otherNotes = it },
                        label = { Text("其他备注") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = isReadonly,
                        minLines = 2,
                        maxLines = 6,
                    )

                    Text(text = "特征标签", style = MaterialTheme.typography.bodySmall)
                    ChipFlowRow(modifier = Modifier.fillMaxWidth()) {
                        featureTags.values.forEach { tag ->
                            val tagColor = tag.displayColor()
                            ColoredFilterChip(
                                selected = tag.id in selectedFeatureTagIds,
                                onClick = { if (!isReadonly) onToggleFeatureTag(tag.id) },
                                label = tag.name,
                                color = tagColor,
                                enabled = !isReadonly,
                                leadingIcon = {
                                    FeatureTagSmallIcon(
                                        tag = tag,
                                        size = 18.dp,
                                        tint = tagColor,
                                    )
                                },
                            )
                        }
                    }

                    if (!isReadonly) {
                        Button(
                            onClick = {
                                onSave(
                                    room.copy(
                                        roomNumber = roomNumber.trim(),
                                        paymentType = paymentType,
                                        paymentMonths = paymentMonths,
                                        livingStatusKey = livingStatusKey,
                                        ownerFeedback = ownerFeedback,
                                        otherNotes = otherNotes,
                                    ),
                                    selectedFeatureTagIds,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("保存")
                        }
                    }
                    if (allowDelete) {
                        TextButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("删除铺位", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

private fun paymentTypeColor(type: PaymentType): Color = when (type) {
    PaymentType.ARREARS -> Color(0xFFEF4444)
    PaymentType.PREPAID -> Color(0xFF059669)
    PaymentType.NORMAL -> Color(0xFF64748B)
}
