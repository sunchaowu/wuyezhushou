package com.guanjiazhushou.assistant.presentation.building

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guanjiazhushou.assistant.domain.model.BuildingKind
import com.guanjiazhushou.assistant.domain.model.OnboardingStep
import com.guanjiazhushou.assistant.domain.model.PaymentType
import com.guanjiazhushou.assistant.presentation.common.ChipFlowRow
import com.guanjiazhushou.assistant.presentation.onboarding.OnboardingAnchor
import com.guanjiazhushou.assistant.presentation.onboarding.OnboardingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingFormScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    onboardingViewModel: OnboardingViewModel? = null,
    viewModel: BuildingFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    uiState.pendingSaveSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = viewModel::dismissSaveConfirmation,
            title = { Text("确认保存变更") },
            text = { Text(summary.toMessage()) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmSave) {
                    Text("确认保存")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissSaveConfirmation) {
                    Text("取消")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            uiState.isReadonly -> when (uiState.buildingKind) {
                                BuildingKind.SHOP -> "查看商铺楼栋"
                                BuildingKind.RESIDENTIAL -> "查看楼栋"
                            }
                            uiState.isEditMode -> when (uiState.buildingKind) {
                                BuildingKind.SHOP -> "编辑商铺楼栋"
                                BuildingKind.RESIDENTIAL -> "编辑楼栋"
                            }
                            else -> when (uiState.buildingKind) {
                                BuildingKind.SHOP -> "新建商铺楼栋"
                                BuildingKind.RESIDENTIAL -> "新建楼栋"
                            }
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val communityField: @Composable () -> Unit = {
                OutlinedTextField(
                    value = uiState.communityName,
                    onValueChange = viewModel::updateCommunityName,
                    label = { Text("小区名称") },
                    placeholder = { Text("如：阳光花园") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = uiState.isReadonly,
                    singleLine = true,
                )
            }
            if (onboardingViewModel != null && !uiState.isReadonly && !uiState.isEditMode) {
                OnboardingAnchor(
                    step = OnboardingStep.FILL_FORM,
                    onboardingViewModel = onboardingViewModel,
                    modifier = Modifier.fillMaxWidth(),
                    content = communityField,
                )
            } else {
                communityField()
            }
            if (!uiState.isReadonly && !uiState.isEditMode) {
                Text(
                    text = "楼栋类型",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ChipFlowRow {
                    BuildingKind.entries.forEach { kind ->
                        FilterChip(
                            selected = uiState.buildingKind == kind,
                            onClick = { viewModel.updateBuildingKind(kind) },
                            label = { Text(kind.label) },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::updateName,
                label = {
                    Text(
                        if (uiState.buildingKind == BuildingKind.SHOP) "商铺楼栋名称" else "楼栋名称",
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                readOnly = uiState.isReadonly,
            )
            OutlinedTextField(
                value = uiState.unitsPerFloor,
                onValueChange = viewModel::updateUnitsPerFloor,
                label = {
                    Text(
                        if (uiState.buildingKind == BuildingKind.SHOP) "每层商铺数" else "每层户数",
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                readOnly = uiState.isReadonly,
            )
            OutlinedTextField(
                value = uiState.startFloor,
                onValueChange = viewModel::updateStartFloor,
                label = { Text("起始楼层") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = uiState.isReadonly,
            )
            OutlinedTextField(
                value = uiState.maxFloor,
                onValueChange = viewModel::updateMaxFloor,
                label = { Text("最高楼层") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = uiState.isReadonly,
            )
            OutlinedTextField(
                value = uiState.refugeFloors,
                onValueChange = viewModel::updateRefugeFloors,
                label = { Text("避难层（逗号分隔）") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = uiState.isReadonly,
            )
            OutlinedTextField(
                value = uiState.excludeRooms,
                onValueChange = viewModel::updateExcludeRooms,
                label = {
                    Text(
                        if (uiState.buildingKind == BuildingKind.SHOP) "剔除铺位" else "剔除房间",
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                readOnly = uiState.isReadonly,
            )
            if (uiState.buildingKind == BuildingKind.SHOP) {
                OutlinedTextField(
                    value = uiState.shopRooms,
                    onValueChange = viewModel::updateShopRooms,
                    label = { Text("商铺编号") },
                    placeholder = { Text("奇数铺位自动生成，可修改") },
                    supportingText = {
                        Text("根据每层商铺数自动生成奇数编号（如 101、103），保存后可于房间维护界面单独调整")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = uiState.isReadonly,
                )
            } else {
                OutlinedTextField(
                    value = uiState.shopRooms,
                    onValueChange = viewModel::updateShopRooms,
                    label = { Text("底部商铺编号（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = uiState.isReadonly,
                )
            }
            if (!uiState.isReadonly) {
                Text(
                    text = if (uiState.isEditMode) "新房间默认缴费状态" else "默认缴费状态",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ChipFlowRow {
                    PaymentType.entries.forEach { type ->
                        FilterChip(
                            selected = uiState.defaultPaymentType == type,
                            onClick = { viewModel.updateDefaultPaymentType(type) },
                            label = { Text(type.label) },
                        )
                    }
                }
            }
            if (uiState.isEditMode && !uiState.isReadonly) {
                Text(
                    text = "保存后将按新参数同步房间：新增楼层会自动补房间，移除的房间号会删除对应数据。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            uiState.errorMessage?.let { message ->
                Text(text = message, color = MaterialTheme.colorScheme.error)
            }
            if (!uiState.isReadonly) {
                Button(
                    onClick = { viewModel.requestSave(onSaved) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                ) {
                    Text(
                        when {
                            uiState.isLoading -> "保存中…"
                            uiState.isEditMode -> "保存并同步房间"
                            else -> "保存并生成房间"
                        },
                    )
                }
            }
        }
    }
}
