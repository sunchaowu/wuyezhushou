package com.guanjiazhushou.assistant.presentation.building

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guanjiazhushou.assistant.domain.model.Building
import com.guanjiazhushou.assistant.domain.model.BuildingMetrics
import com.guanjiazhushou.assistant.domain.model.OnboardingStep
import com.guanjiazhushou.assistant.domain.model.SourceType
import com.guanjiazhushou.assistant.presentation.common.CollectionProgressBackground
import com.guanjiazhushou.assistant.presentation.onboarding.OnboardingAnchor
import com.guanjiazhushou.assistant.presentation.onboarding.OnboardingViewModel
import com.guanjiazhushou.assistant.presentation.common.MetricCard
import com.guanjiazhushou.assistant.presentation.common.formatPercent
import com.guanjiazhushou.assistant.ui.theme.AppThemeExtras

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingListScreen(
    onCreateBuilding: () -> Unit,
    onEditBuilding: (Long) -> Unit,
    onOpenGrid: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onboardingViewModel: OnboardingViewModel? = null,
    viewModel: BuildingListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val buildings = uiState.buildings
    val buildingGroups = uiState.buildingGroups
    val palette = AppThemeExtras.palette
    val firstBuildingId = buildingGroups.firstOrNull()?.buildings?.firstOrNull()?.id

    var actionBuilding by remember { mutableStateOf<Building?>(null) }
    var deleteTarget by remember { mutableStateOf<Building?>(null) }

    actionBuilding?.let { building ->
        AlertDialog(
            onDismissRequest = { actionBuilding = null },
            title = { Text(building.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("选择对该楼栋的操作")
                    TextButton(
                        onClick = {
                            actionBuilding = null
                            onEditBuilding(building.id)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("修改楼栋参数")
                    }
                    TextButton(
                        onClick = {
                            actionBuilding = null
                            deleteTarget = building
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("删除楼栋", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { actionBuilding = null }) {
                    Text("取消")
                }
            },
        )
    }

    deleteTarget?.let { building ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除楼栋") },
            text = {
                Text(
                    "确定删除「${building.name}」吗？该楼栋下的全部房屋、沟通记录及标记数据将一并删除，且不可恢复。",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBuilding(building.id)
                        deleteTarget = null
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("取消")
                }
            },
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("操作失败") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::clearError) {
                    Text("确定")
                }
            },
        )
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("楼栋") },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                },
            )
        },
        floatingActionButton = {
            if (buildings.isNotEmpty()) {
                val fabContent: @Composable () -> Unit = {
                    FloatingActionButton(onClick = onCreateBuilding) {
                        Icon(Icons.Default.Add, contentDescription = "新建楼栋")
                    }
                }
                if (onboardingViewModel != null) {
                    OnboardingAnchor(
                        step = OnboardingStep.CREATE_BUILDING,
                        onboardingViewModel = onboardingViewModel,
                        content = fabContent,
                    )
                } else {
                    fabContent()
                }
            }
        },
    ) { padding ->
        if (buildings.isEmpty()) {
            BuildingEmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onCreateBuilding = onCreateBuilding,
                onboardingViewModel = onboardingViewModel,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "summary") {
                    BuildingSummaryRow(metrics = uiState.summary)
                }
                buildingGroups.forEach { group ->
                    item(key = "community_header_${group.communityId}") {
                        Text(
                            text = group.communityName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                    }
                    items(group.buildings, key = { it.id }) { building ->
                        val itemContent: @Composable () -> Unit = {
                            BuildingListItem(
                                building = building,
                                metrics = uiState.metricsByBuildingId[building.id],
                                progressFillColor = palette.collectionProgressFill,
                                progressTrackColor = palette.collectionProgressTrack,
                                readonlyBadgeColor = palette.readonlyBadge,
                                onClick = { onOpenGrid(building.id) },
                                onLongClick = {
                                    if (!building.isReadonly) {
                                        actionBuilding = building
                                    }
                                },
                            )
                        }
                        if (
                            onboardingViewModel != null &&
                            building.id == firstBuildingId
                        ) {
                            OnboardingAnchor(
                                step = OnboardingStep.MODIFY_DELETE,
                                onboardingViewModel = onboardingViewModel,
                                modifier = Modifier.fillMaxWidth(),
                                content = itemContent,
                            )
                        } else {
                            itemContent()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildingEmptyState(
    modifier: Modifier = Modifier,
    onCreateBuilding: () -> Unit,
    onboardingViewModel: OnboardingViewModel? = null,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Apartment,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "暂无楼栋数据",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "新建楼栋并生成房间数据，或加入临时组网与同事对齐数据",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        val fabContent: @Composable () -> Unit = {
            FloatingActionButton(onClick = onCreateBuilding) {
                Icon(Icons.Default.Add, contentDescription = "新建楼栋")
            }
        }
        if (onboardingViewModel != null) {
            OnboardingAnchor(
                step = OnboardingStep.CREATE_BUILDING,
                onboardingViewModel = onboardingViewModel,
                content = fabContent,
            )
        } else {
            fabContent()
        }
        Text(
            text = "+ 新建楼栋",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun BuildingSummaryRow(metrics: BuildingMetrics) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetricCard(
            title = "入住率",
            value = formatPercent(metrics.occupancyRate),
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            title = "收缴率",
            value = formatPercent(metrics.collectionRate),
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            title = "欠费户",
            value = "${metrics.arrearsCount}户",
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BuildingListItem(
    building: Building,
    metrics: BuildingMetrics?,
    progressFillColor: Color,
    progressTrackColor: Color,
    readonlyBadgeColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val collectionProgress = (metrics?.collectionRate ?: 0f) / 100f
    val progressBarColor = MaterialTheme.colorScheme.primary

    CollectionProgressBackground(
        progress = collectionProgress,
        fillColor = progressFillColor,
        trackColor = progressTrackColor,
        progressBarColor = progressBarColor,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp)) {
            Text(text = building.name, style = MaterialTheme.typography.titleLarge)
            Text(
                text = "${building.startFloor}-${building.maxFloor}层 · 每层${building.unitsPerFloor}户",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            metrics?.let {
                Text(
                    text = "入住率 ${formatPercent(it.occupancyRate)} · 收缴率 ${formatPercent(it.collectionRate)} · 欠费 ${it.arrearsCount}户",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (building.sourceType == SourceType.IMPORTED) {
                Text(
                    text = "只读 · 来自 ${building.sourceDevice.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = readonlyBadgeColor,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
