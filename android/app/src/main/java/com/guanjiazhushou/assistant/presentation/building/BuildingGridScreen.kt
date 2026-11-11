package com.guanjiazhushou.assistant.presentation.building

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guanjiazhushou.assistant.domain.model.BuildingKind
import com.guanjiazhushou.assistant.domain.model.BatchFeatureTagMode
import com.guanjiazhushou.assistant.domain.model.BatchRoomUpdateParams
import com.guanjiazhushou.assistant.domain.model.FeatureTag
import com.guanjiazhushou.assistant.domain.model.LivingStatusSetting
import com.guanjiazhushou.assistant.domain.model.PaymentType
import com.guanjiazhushou.assistant.domain.model.RoomType
import com.guanjiazhushou.assistant.domain.model.Room
import com.guanjiazhushou.assistant.domain.model.RoomWithFeatureIds
import com.guanjiazhushou.assistant.presentation.common.ChipFlowRow
import com.guanjiazhushou.assistant.presentation.common.MetricCard
import com.guanjiazhushou.assistant.presentation.common.FeatureTagSmallIcon
import com.guanjiazhushou.assistant.presentation.common.displayColor
import com.guanjiazhushou.assistant.presentation.common.asRoomCellBackground
import com.guanjiazhushou.assistant.presentation.common.formatPercent
import com.guanjiazhushou.assistant.presentation.common.livingStatusColor
import com.guanjiazhushou.assistant.presentation.common.livingStatusLabel
import com.guanjiazhushou.assistant.presentation.common.toStatusColorMap
import com.guanjiazhushou.assistant.presentation.common.toStatusLabelMap
import com.guanjiazhushou.assistant.presentation.reminder.ReminderPermission
import com.guanjiazhushou.assistant.presentation.voice.ListenProposalSheet
import com.guanjiazhushou.assistant.presentation.voice.ListenStatusBar
import com.guanjiazhushou.assistant.presentation.voice.ListenTextSimDialog
import com.guanjiazhushou.assistant.presentation.voice.RoomDisambiguationDialog
import com.guanjiazhushou.assistant.ui.theme.SemanticColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingGridScreen(
    onBack: () -> Unit,
    onOpenCommunication: (Long) -> Unit,
    viewModel: BuildingGridViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listenState by viewModel.listenState.collectAsStateWithLifecycle()
    val listenEvent by viewModel.listenUiEvent.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showTextSim by remember { mutableStateOf(false) }
    var micPermissionAsked by remember { mutableStateOf(false) }
    var micRetryAfterGrant by remember { mutableStateOf(false) }
    var notificationAsked by remember { mutableStateOf(false) }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && !micRetryAfterGrant) {
            micRetryAfterGrant = true
            viewModel.retryListenAsr()
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* reminder schedule surfaces remaining exact-alarm errors */ }
    fun ensureReminderPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !ReminderPermission.hasPostNotifications(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return false
        }
        if (!ReminderPermission.canScheduleExactAlarms(context)) {
            context.startActivity(ReminderPermission.exactAlarmSettingsIntent(context))
            return false
        }
        return true
    }
    LaunchedEffect(listenState.isListening, listenState.isMuted) {
        if (!listenState.isListening || listenState.isMuted) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted && !micPermissionAsked) {
            micPermissionAsked = true
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !ReminderPermission.hasPostNotifications(context) &&
            !notificationAsked
        ) {
            notificationAsked = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val statusColorMap = remember(uiState.livingStatusSettings) {
        uiState.livingStatusSettings.toStatusColorMap()
    }
    val statusLabelMap = remember(uiState.livingStatusSettings) {
        uiState.livingStatusSettings.toStatusLabelMap()
    }
    val building = uiState.building
    val isReadonly = building?.isReadonly == true

    val canManageBottomShops =
        building?.buildingKind == BuildingKind.RESIDENTIAL && !isReadonly

    LaunchedEffect(listenEvent.navigateRoomId, listenEvent.proposals) {
        val roomId = listenEvent.navigateRoomId ?: return@LaunchedEffect
        if (listenEvent.proposals.isNotEmpty()) return@LaunchedEffect
        onOpenCommunication(roomId)
        viewModel.consumeListenNavigation()
    }

    if (showTextSim) {
        ListenTextSimDialog(
            onDismiss = { showTextSim = false },
            onSubmit = viewModel::processListenText,
        )
    }

    if (listenEvent.ambiguousRoomNumbers.isNotEmpty()) {
        RoomDisambiguationDialog(
            roomNumbers = listenEvent.ambiguousRoomNumbers,
            onSelect = { number ->
                viewModel.resolveRoomId(number)?.let(onOpenCommunication)
                viewModel.consumeListenAmbiguous()
            },
            onDismiss = viewModel::consumeListenAmbiguous,
        )
    }

    if (listenEvent.proposals.isNotEmpty()) {
        ListenProposalSheet(
            proposals = listenEvent.proposals,
            intent = listenEvent.intent,
            onConfirmPayment = viewModel::confirmPaymentProposal,
            onConfirmEvent = viewModel::confirmEventProposal,
            onConfirmTags = viewModel::confirmFeatureTagProposal,
            onSchedulePaymentFollowUp = { proposal, type, months, triggerAt ->
                if (!ensureReminderPermissions()) return@ListenProposalSheet
                val roomId = viewModel.resolveRoomId(proposal.roomNumber) ?: return@ListenProposalSheet
                viewModel.schedulePaymentFollowUpAfterListen(roomId, type, months, triggerAt)
            },
            onDismiss = {
                val pendingNav = listenEvent.navigateRoomId
                viewModel.consumeListenProposals()
                if (pendingNav != null) {
                    onOpenCommunication(pendingNav)
                    viewModel.consumeListenNavigation()
                }
            },
        )
    }

    if (uiState.showAddBottomShopDialog) {
        AddBottomShopDialog(
            errorMessage = uiState.operationError,
            onDismiss = viewModel::dismissAddBottomShopDialog,
            onConfirm = viewModel::addBottomShop,
        )
    }

    uiState.operationError?.let { message ->
        if (!uiState.showAddBottomShopDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissOperationError,
                title = { Text("操作失败") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissOperationError) {
                        Text("确定")
                    }
                },
            )
        }
    }

    if (uiState.showBatchSettingsDialog) {
        BatchRoomSettingsDialog(
            selectedCount = uiState.selectedRoomIds.size,
            livingStatusSettings = uiState.livingStatusSettings,
            featureTags = uiState.featureTags.values.toList(),
            onDismiss = viewModel::dismissBatchSettingsDialog,
            onConfirm = viewModel::applyBatchSettings,
            onDelete = {
                viewModel.dismissBatchSettingsDialog()
                viewModel.batchDeleteSelectedRooms()
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            uiState.isSelectionMode -> "已选 ${uiState.selectedRoomIds.size} 间"
                            else -> building?.name ?: "楼栋网格"
                        },
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.isSelectionMode) {
                                viewModel.exitSelectionMode()
                            } else {
                                onBack()
                            }
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (uiState.isSelectionMode && !isReadonly) {
                        TextButton(
                            onClick = viewModel::openBatchSettingsDialog,
                            enabled = uiState.selectedRoomIds.isNotEmpty(),
                        ) {
                            Text("批量设置")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ListenStatusBar(
                isListening = listenState.isListening,
                isMuted = listenState.isMuted,
                errorMessage = listenState.lastError,
                liveTranscript = listenState.liveTranscript,
                onToggleMute = viewModel::toggleListenMute,
                onOpenTextSim = { showTextSim = true },
                onClearError = viewModel::clearListenError,
            )
            listenState.lastError?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (!isReadonly && !uiState.isSelectionMode) {
                Text(
                    text = "单击编辑房屋，双击进入沟通记录，长按滑动可批量多选",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetricCard(
                    title = "入住率",
                    value = formatPercent(uiState.metrics.occupancyRate),
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = "收缴率",
                    value = formatPercent(uiState.metrics.collectionRate),
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = "欠费户",
                    value = "${uiState.metrics.arrearsCount}户",
                    modifier = Modifier.weight(1f),
                )
            }

            val isShopBuilding = building?.buildingKind == BuildingKind.SHOP
            val residentialRooms = uiState.rooms.filter { it.room.roomType == RoomType.RESIDENTIAL }
            val bottomShopRooms = if (isShopBuilding) {
                emptyList()
            } else {
                uiState.rooms.filter { it.room.roomType == RoomType.SHOP }
            }
            val gridRooms = if (isShopBuilding) uiState.rooms else residentialRooms
            val floors = gridRooms.map { it.room.floor }.distinct().sortedDescending()
            val roomBounds = remember { mutableStateMapOf<Long, Rect>() }
            var gridCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .onGloballyPositioned { gridCoordinates = it }
                        .roomGridDragSelect(
                            enabled = !isReadonly,
                            roomBounds = roomBounds,
                            onSelectionStart = viewModel::startDragSelection,
                            onSelectionAdd = viewModel::addRoomToSelection,
                        ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    floors.forEach { floor ->
                        val floorRooms = gridRooms.filter { it.room.floor == floor }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "${floor}F",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            RoomGridFlow(
                                rooms = floorRooms,
                                gridCoordinates = gridCoordinates,
                                roomBounds = roomBounds,
                                statusColorMap = statusColorMap,
                                statusLabelMap = statusLabelMap,
                                featureTags = uiState.featureTags,
                                selectedRoomIds = uiState.selectedRoomIds,
                                isSelectionMode = uiState.isSelectionMode,
                            onRoomClick = viewModel::onRoomClick,
                            onRoomDoubleClick = { roomWithFeatures ->
                                    if (!uiState.isSelectionMode) {
                                        onOpenCommunication(roomWithFeatures.room.id)
                                    }
                                },
                            )
                        }
                    }

                    if (bottomShopRooms.isNotEmpty() || canManageBottomShops) {
                        Text(
                            text = "商铺",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        BottomShopGridRow(
                            shopRooms = bottomShopRooms,
                            showAddButton = canManageBottomShops && !uiState.isSelectionMode,
                            gridCoordinates = gridCoordinates,
                            roomBounds = roomBounds,
                            statusColorMap = statusColorMap,
                            statusLabelMap = statusLabelMap,
                            featureTags = uiState.featureTags,
                            selectedRoomIds = uiState.selectedRoomIds,
                            isSelectionMode = uiState.isSelectionMode,
                            onRoomClick = viewModel::onRoomClick,
                            onRoomDoubleClick = { roomWithFeatures ->
                                if (!uiState.isSelectionMode) {
                                    onOpenCommunication(roomWithFeatures.room.id)
                                }
                            },
                            onAddClick = viewModel::openAddBottomShopDialog,
                        )
                    }
                }
            }
        }
    }

    if (!uiState.isSelectionMode) {
        uiState.selectedRoom?.let { room ->
            androidx.compose.runtime.key(room.id) {
                val isBottomShop = building?.buildingKind == BuildingKind.RESIDENTIAL &&
                    room.roomType == RoomType.SHOP &&
                    room.floor == 0
                RoomDetailSheet(
                    room = room,
                    livingStatusSettings = uiState.livingStatusSettings,
                    featureTags = uiState.featureTags,
                    selectedFeatureTagIds = uiState.selectedFeatureTagIds,
                    isReadonly = isReadonly,
                    allowDelete = isBottomShop && !isReadonly,
                    onDismiss = viewModel::dismissRoomDetail,
                    onSave = { updatedRoom, tagIds ->
                        viewModel.updateSelectedRoom(updatedRoom, tagIds)
                    },
                    onDelete = { viewModel.removeBottomShop(room.id) },
                    onToggleFeatureTag = viewModel::toggleFeatureTag,
                    onSchedulePaymentFollowUp = if (isReadonly) {
                        null
                    } else {
                        { triggerAt ->
                            if (ensureReminderPermissions()) {
                                viewModel.schedulePaymentFollowUpReminder(room, triggerAt)
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun BatchRoomSettingsDialog(
    selectedCount: Int,
    livingStatusSettings: List<LivingStatusSetting>,
    featureTags: List<FeatureTag>,
    onDismiss: () -> Unit,
    onConfirm: (BatchRoomUpdateParams) -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除房屋") },
            text = {
                Text(
                    "确定删除已选 $selectedCount 间房屋？关联沟通记录将一并删除，此操作不可恢复。",
                )
            },
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
    var updatePayment by remember { mutableStateOf(false) }
    var paymentType by remember { mutableStateOf(PaymentType.ARREARS) }
    var paymentMonths by remember { mutableIntStateOf(1) }

    var updateLivingStatus by remember { mutableStateOf(false) }
    var livingStatusKey by remember {
        mutableStateOf(livingStatusSettings.firstOrNull()?.statusKey.orEmpty())
    }

    var updateFeatureTags by remember { mutableStateOf(false) }
    var featureTagMode by remember { mutableStateOf(BatchFeatureTagMode.REPLACE) }
    var selectedTagIds by remember { mutableStateOf(setOf<Long>()) }

    val params = BatchRoomUpdateParams(
        updatePayment = updatePayment,
        paymentType = if (updatePayment) paymentType else null,
        paymentMonths = if (updatePayment) paymentMonths else null,
        updateLivingStatus = updateLivingStatus,
        livingStatusKey = if (updateLivingStatus) livingStatusKey else null,
        updateFeatureTags = updateFeatureTags,
        featureTagMode = featureTagMode,
        featureTagIds = selectedTagIds.toList(),
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {},
                    ),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "批量参数设置",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "已选 $selectedCount 间，开启需要修改的项目后保存",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Column(
                        modifier = Modifier
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        BatchSectionToggle(
                            title = "缴费状态",
                            checked = updatePayment,
                            onCheckedChange = { updatePayment = it },
                        ) {
                            ChipFlowRow {
                                PaymentType.entries.forEach { type ->
                                    FilterChip(
                                        selected = paymentType == type,
                                        onClick = { paymentType = type },
                                        label = { Text(type.label) },
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = paymentMonths.toString(),
                                onValueChange = { value ->
                                    paymentMonths = value.toIntOrNull()?.coerceAtLeast(1) ?: paymentMonths
                                },
                                label = { Text("月数") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                        }

                        BatchSectionToggle(
                            title = "居住状态",
                            checked = updateLivingStatus,
                            onCheckedChange = { updateLivingStatus = it },
                        ) {
                            ChipFlowRow {
                                livingStatusSettings.forEach { status ->
                                    FilterChip(
                                        selected = livingStatusKey == status.statusKey,
                                        onClick = { livingStatusKey = status.statusKey },
                                        label = { Text(status.label) },
                                    )
                                }
                            }
                        }

                        BatchSectionToggle(
                            title = "特征标签",
                            checked = updateFeatureTags,
                            onCheckedChange = { updateFeatureTags = it },
                        ) {
                            Text(
                                text = "操作方式",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            ChipFlowRow {
                                BatchFeatureTagMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = featureTagMode == mode,
                                        onClick = { featureTagMode = mode },
                                        label = { Text(mode.label) },
                                    )
                                }
                            }
                            if (featureTags.isEmpty()) {
                                Text(
                                    text = "暂无特征标签，请先在基础设置中添加",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                ChipFlowRow {
                                    featureTags.forEach { tag ->
                                        FilterChip(
                                            selected = tag.id in selectedTagIds,
                                            onClick = {
                                                selectedTagIds = if (tag.id in selectedTagIds) {
                                                    selectedTagIds - tag.id
                                                } else {
                                                    selectedTagIds + tag.id
                                                }
                                            },
                                            label = { Text(tag.name) },
                                        )
                                    }
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "删除房屋",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = "删除多余或错误的房间记录。底部商铺会同步更新编号列表；再次保存楼栋参数时，未加入剔除列表的住宅房间可能重新生成。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "删除已选 $selectedCount 间房屋",
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("取消")
                        }
                        TextButton(
                            onClick = { onConfirm(params) },
                            enabled = params.isValid,
                        ) {
                            Text("保存")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchSectionToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
        if (checked) {
            content()
        }
    }
}

@Composable
private fun AddBottomShopDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var roomNumber by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增底部商铺") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = roomNumber,
                    onValueChange = { roomNumber = it },
                    label = { Text("商铺编号") },
                    placeholder = { Text("如 S01、铺-01") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                errorMessage?.let { message ->
                    Text(text = message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(roomNumber) },
                enabled = roomNumber.isNotBlank(),
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun BottomShopGridRow(
    shopRooms: List<RoomWithFeatureIds>,
    showAddButton: Boolean,
    gridCoordinates: LayoutCoordinates?,
    roomBounds: MutableMap<Long, Rect>,
    statusColorMap: Map<String, Color>,
    statusLabelMap: Map<String, String>,
    featureTags: Map<Long, FeatureTag>,
    selectedRoomIds: Set<Long>,
    isSelectionMode: Boolean,
    onRoomClick: (RoomWithFeatureIds) -> Unit,
    onRoomDoubleClick: (RoomWithFeatureIds) -> Unit,
    onAddClick: () -> Unit,
) {
    RoomGridFlow(
        rooms = shopRooms,
        gridCoordinates = gridCoordinates,
        roomBounds = roomBounds,
        statusColorMap = statusColorMap,
        statusLabelMap = statusLabelMap,
        featureTags = featureTags,
        selectedRoomIds = selectedRoomIds,
        isSelectionMode = isSelectionMode,
        onRoomClick = onRoomClick,
        onRoomDoubleClick = onRoomDoubleClick,
        trailingContent = if (showAddButton) {
            { AddShopCell(onClick = onAddClick) }
        } else {
            null
        },
    )
}

@Composable
private fun AddShopCell(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .size(RoomCellSize)
            .clip(shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "新增商铺",
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun RoomGridFlow(
    rooms: List<RoomWithFeatureIds>,
    gridCoordinates: LayoutCoordinates?,
    roomBounds: MutableMap<Long, Rect>,
    statusColorMap: Map<String, Color>,
    statusLabelMap: Map<String, String>,
    featureTags: Map<Long, com.guanjiazhushou.assistant.domain.model.FeatureTag>,
    selectedRoomIds: Set<Long>,
    isSelectionMode: Boolean,
    onRoomClick: (RoomWithFeatureIds) -> Unit,
    onRoomDoubleClick: (RoomWithFeatureIds) -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    ChipFlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalSpacing = 6.dp,
        verticalSpacing = 6.dp,
    ) {
        rooms.forEach { roomWithFeatures ->
            RoomCell(
                roomWithFeatures = roomWithFeatures,
                statusColorMap = statusColorMap,
                statusLabelMap = statusLabelMap,
                featureTags = featureTags,
                isSelected = roomWithFeatures.room.id in selectedRoomIds,
                isSelectionMode = isSelectionMode,
                onClick = { onRoomClick(roomWithFeatures) },
                onDoubleClick = { onRoomDoubleClick(roomWithFeatures) },
                onBoundsChanged = { rect ->
                    roomBounds[roomWithFeatures.room.id] = rect
                },
                gridCoordinates = gridCoordinates,
            )
        }
        trailingContent?.invoke()
    }
}

private val RoomCellSize = 78.dp
private val RoomCellPadding = 5.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoomCell(
    roomWithFeatures: RoomWithFeatureIds,
    statusColorMap: Map<String, Color>,
    statusLabelMap: Map<String, String>,
    featureTags: Map<Long, com.guanjiazhushou.assistant.domain.model.FeatureTag>,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onBoundsChanged: (Rect) -> Unit,
    gridCoordinates: LayoutCoordinates?,
    modifier: Modifier = Modifier,
) {
    val room = roomWithFeatures.room
    val livingBackground = statusColorMap
        .livingStatusColor(room.livingStatusKey)
        .asRoomCellBackground()
    val paymentColor = when (room.paymentType) {
        PaymentType.ARREARS -> SemanticColors.Error
        PaymentType.PREPAID -> SemanticColors.Success
        PaymentType.NORMAL -> SemanticColors.Neutral
    }
    val shape = RoundedCornerShape(8.dp)
    val selectionBorder = if (isSelected) {
        Modifier.border(width = 2.dp, color = MaterialTheme.colorScheme.primary, shape = shape)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(RoomCellSize)
            .onGloballyPositioned { coordinates ->
                val parent = gridCoordinates
                if (parent != null && parent.isAttached && coordinates.isAttached) {
                    onBoundsChanged(parent.localBoundingBoxOf(coordinates, clipBounds = false))
                }
            }
            .clip(shape)
            .then(selectionBorder)
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    livingBackground
                },
            )
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleClick,
            )
            .padding(RoomCellPadding),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(11.dp)
                .clip(CircleShape)
                .background(paymentColor),
        )
        Text(
            text = statusLabelMap.livingStatusLabel(room.livingStatusKey),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.TopEnd),
        )
        Text(
            text = room.roomNumber,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.align(Alignment.Center),
        )
        Row(
            modifier = Modifier.align(Alignment.BottomStart),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            roomWithFeatures.featureTagIds.take(3).forEach { tagId ->
                featureTags[tagId]?.let { tag ->
                    FeatureTagSmallIcon(
                        tag = tag,
                        size = 14.dp,
                        tint = tag.displayColor(),
                    )
                }
            }
            if (roomWithFeatures.featureTagIds.size > 3) {
                Text(
                    text = "+${roomWithFeatures.featureTagIds.size - 3}",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
        if (isSelectionMode && isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "✓", color = Color.White, fontSize = 9.sp)
            }
        }
    }
}
