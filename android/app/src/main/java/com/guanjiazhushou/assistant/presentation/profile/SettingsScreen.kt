package com.guanjiazhushou.assistant.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guanjiazhushou.assistant.domain.model.FeatureTag
import com.guanjiazhushou.assistant.presentation.common.ChipFlowRow
import com.guanjiazhushou.assistant.presentation.common.FeatureTagSmallIcon
import com.guanjiazhushou.assistant.domain.model.IconType
import com.guanjiazhushou.assistant.domain.model.LivingStatusColorDefaults
import com.guanjiazhushou.assistant.domain.model.LivingStatusSetting
import com.guanjiazhushou.assistant.ui.theme.AppThemeExtras
import androidx.compose.ui.graphics.Color

private val presetFeatureEmojis = listOf("👴", "👶", "🐕", "🏠", "🚗", "🌟", "❤️", "📌")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val palette = AppThemeExtras.palette
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val livingSettings by viewModel.livingSettings.collectAsStateWithLifecycle()
    val featureTags by viewModel.featureTags.collectAsStateWithLifecycle()
    val featureMessage by viewModel.featureMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddFeatureDialog by rememberSaveable { mutableStateOf(false) }
    var showAddLivingStatusDialog by rememberSaveable { mutableStateOf(false) }
    var editingLivingStatus by remember { mutableStateOf<LivingStatusSetting?>(null) }
    var editingFeatureTag by remember { mutableStateOf<FeatureTag?>(null) }

    LaunchedEffect(featureMessage) {
        featureMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearFeatureMessage()
        }
    }

    editingLivingStatus?.let { setting ->
        LivingStatusFormDialog(
            initial = setting,
            onDismiss = { editingLivingStatus = null },
            onConfirm = { label, colorArgb, countsAsOccupied ->
                viewModel.updateLivingStatus(setting, label, colorArgb, countsAsOccupied)
                editingLivingStatus = null
            },
        )
    }

    editingFeatureTag?.let { tag ->
        FeatureTagFormDialog(
            initial = tag,
            onDismiss = { editingFeatureTag = null },
            onConfirm = { name, emoji, description ->
                viewModel.updateFeatureTag(tag, name, emoji, description)
                editingFeatureTag = null
            },
        )
    }

    if (showAddFeatureDialog) {
        FeatureTagFormDialog(
            onDismiss = { showAddFeatureDialog = false },
            onConfirm = { name, emoji, description ->
                viewModel.addFeatureTag(name, emoji, description)
                showAddFeatureDialog = false
            },
        )
    }

    if (showAddLivingStatusDialog) {
        LivingStatusFormDialog(
            onDismiss = { showAddLivingStatusDialog = false },
            onConfirm = { label, colorArgb, countsAsOccupied ->
                viewModel.addLivingStatus(label, colorArgb, countsAsOccupied)
                showAddLivingStatusDialog = false
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("基础设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            when (selectedTab) {
                SettingsTab.LIVING_STATUS -> {
                    FloatingActionButton(onClick = { showAddLivingStatusDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加居住状态")
                    }
                }
                SettingsTab.FEATURE_PROFILE -> {
                    FloatingActionButton(onClick = { showAddFeatureDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加特征")
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                SettingsTab.entries.forEachIndexed { index, tab ->
                    SegmentedButton(
                        selected = selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        shape = SegmentedButtonDefaults.itemShape(index, SettingsTab.entries.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = palette.primary,
                            activeContentColor = palette.surface,
                            inactiveContainerColor = palette.surface,
                            inactiveContentColor = palette.textSecondary,
                            activeBorderColor = palette.primary,
                            inactiveBorderColor = palette.surfaceVariant,
                        ),
                    ) {
                        Text(tab.label)
                    }
                }
            }

            when (selectedTab) {
                SettingsTab.LIVING_STATUS -> LivingStatusTab(
                    settings = livingSettings,
                    onEdit = { editingLivingStatus = it },
                    onToggleOccupied = viewModel::toggleOccupied,
                    onDelete = viewModel::deleteLivingStatus,
                )
                SettingsTab.FEATURE_PROFILE -> FeatureProfileTab(
                    featureTags = featureTags,
                    onEdit = { editingFeatureTag = it },
                    onDelete = viewModel::deleteFeatureTag,
                )
            }
        }
    }
}

@Composable
private fun LivingStatusTab(
    settings: List<LivingStatusSetting>,
    onEdit: (LivingStatusSetting) -> Unit,
    onToggleOccupied: (String, Boolean) -> Unit,
    onDelete: (LivingStatusSetting) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<LivingStatusSetting?>(null) }

    pendingDelete?.let { setting ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除居住状态") },
            text = { Text("确定删除「${setting.label}」？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(setting)
                        pendingDelete = null
                    },
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消")
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
    ) {
        item {
            Text(
                text = "居住状态管理",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Text(
                text = "点击条目可编辑名称与颜色；开关控制是否计入入住率。房间网格将按颜色区分居住状态。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        if (settings.isEmpty()) {
            item {
                Text(
                    text = "暂无居住状态，点击右下角 + 添加",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            items(settings, key = { it.statusKey }) { setting ->
                ListItem(
                    headlineContent = { Text(setting.label) },
                    supportingContent = {
                        Text(
                            text = if (setting.isBuiltin) "系统预设" else "自定义",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(setting.colorArgb.toInt()))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = CircleShape,
                                ),
                        )
                    },
                    trailingContent = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (!setting.isBuiltin) {
                                IconButton(onClick = { pendingDelete = setting }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "删除",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                            Switch(
                                checked = setting.countsAsOccupied,
                                onCheckedChange = { checked ->
                                    onToggleOccupied(setting.statusKey, checked)
                                },
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEdit(setting) },
                )
            }
        }
        item {
            Text(
                text = "入住率 = 已开启计入入住状态的户数 / 总户数",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun FeatureProfileTab(
    featureTags: List<FeatureTag>,
    onEdit: (FeatureTag) -> Unit,
    onDelete: (FeatureTag) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<FeatureTag?>(null) }

    pendingDelete?.let { tag ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除特征") },
            text = { Text("确定删除「${tag.name}」？已关联的房屋将移除该特征标记。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(tag)
                        pendingDelete = null
                    },
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消")
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
    ) {
        item {
            Text(
                text = "用户特征画像",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Text(
                text = "点击条目可编辑名称、图标与描述。名称尽量短；若口语说法与名称不一致，请填写描述性介绍，便于听记语音识别。内置标签不可删除。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        if (featureTags.isEmpty()) {
            item {
                Text(
                    text = "暂无特征标签，点击右下角 + 添加",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            items(featureTags, key = { it.id }) { tag ->
                FeatureTagListItem(
                    tag = tag,
                    onClick = { onEdit(tag) },
                    onDelete = { pendingDelete = tag },
                )
            }
        }
    }
}

@Composable
private fun FeatureTagListItem(
    tag: FeatureTag,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(tag.name) },
        supportingContent = {
            Text(
                text = if (tag.isBuiltin) "内置" else "自定义",
                style = MaterialTheme.typography.bodySmall,
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                FeatureTagSmallIcon(tag = tag, size = 24.dp)
            }
        },
        trailingContent = {
            if (!tag.isBuiltin) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

@Composable
private fun LivingStatusFormDialog(
    initial: LivingStatusSetting? = null,
    onDismiss: () -> Unit,
    onConfirm: (label: String, colorArgb: Long, countsAsOccupied: Boolean) -> Unit,
) {
    val isEdit = initial != null

    var label by remember(initial?.statusKey) { mutableStateOf(initial?.label.orEmpty()) }
    var selectedColorArgb by remember(initial?.statusKey) {
        mutableStateOf(initial?.colorArgb ?: LivingStatusColorDefaults.presetOptions.first())
    }
    var countsAsOccupied by remember(initial?.statusKey) {
        mutableStateOf(initial?.countsAsOccupied ?: false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "编辑居住状态" else "添加居住状态") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("状态名称") },
                    placeholder = { Text("如：短租、装修中") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                LivingStatusColorPicker(
                    selectedColorArgb = selectedColorArgb,
                    onColorSelected = { selectedColorArgb = it },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "计入入住统计")
                    Switch(
                        checked = countsAsOccupied,
                        onCheckedChange = { countsAsOccupied = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(label, selectedColorArgb, countsAsOccupied) },
                enabled = label.trim().isNotEmpty(),
            ) {
                Text(if (isEdit) "保存" else "添加")
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
private fun LivingStatusColorPicker(
    selectedColorArgb: Long,
    onColorSelected: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "显示颜色",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ChipFlowRow {
            LivingStatusColorDefaults.presetOptions.forEach { colorArgb ->
                val color = Color(colorArgb.toInt())
                val selected = selectedColorArgb == colorArgb
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            },
                            shape = CircleShape,
                        )
                        .clickable { onColorSelected(colorArgb) },
                )
            }
        }
    }
}

@Composable
private fun FeatureTagFormDialog(
    initial: FeatureTag? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String, description: String) -> Unit,
) {
    val isEdit = initial != null
    var name by remember(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember(initial?.id) {
        mutableStateOf(initial?.description.orEmpty())
    }
    var selectedEmoji by remember(initial?.id) {
        mutableStateOf(
            if (initial?.iconType == IconType.EMOJI) initial.iconKey.orEmpty() else "",
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "编辑特征标签" else "添加特征标签") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("特征名称") },
                    placeholder = { Text("如：独居、残障关怀") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述性介绍（可选）") },
                    placeholder = { Text("如：家里请了住家保姆或护工长期居住") },
                    supportingText = {
                        Text("口语说法与名称不一致时填写，供听记语义匹配")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                )
                if (initial?.isBuiltin == true && initial.iconType == IconType.BUILTIN) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            FeatureTagSmallIcon(tag = initial, size = 24.dp)
                        }
                        Text(
                            text = "内置图标（不可修改）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Text(
                        text = "图标（可选）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ChipFlowRow(modifier = Modifier.fillMaxWidth()) {
                        presetFeatureEmojis.forEach { emoji ->
                            FilterChip(
                                selected = selectedEmoji == emoji,
                                onClick = { selectedEmoji = emoji },
                                label = { Text(emoji) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = selectedEmoji,
                        onValueChange = { selectedEmoji = it },
                        label = { Text("自定义图标") },
                        placeholder = { Text("输入 Emoji 或留空使用名称首字") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, selectedEmoji, description) },
                enabled = name.trim().isNotEmpty(),
            ) {
                Text(if (isEdit) "保存" else "添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}
