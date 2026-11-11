package com.guanjiazhushou.assistant.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guanjiazhushou.assistant.domain.model.Building
import com.guanjiazhushou.assistant.presentation.common.rememberBluetoothPermissionGranted
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(
    onBack: () -> Unit,
    viewModel: GroupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val localBuildings by viewModel.localBuildings.collectAsStateWithLifecycle()
    val activeGroup = uiState.activeGroup
    val bluetoothGranted = rememberBluetoothPermissionGranted()

    LaunchedEffect(bluetoothGranted) {
        if (bluetoothGranted) {
            viewModel.refreshPairedDevices()
        }
    }

    uiState.receiveTarget?.let { target ->
        AlertDialog(
            onDismissRequest = viewModel::dismissReceiveDialog,
            title = { Text("接收楼栋分享（局域网）") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "来自 ${parseSenderName(target.manifestJson)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = uiState.receiveCode,
                        onValueChange = viewModel::updateReceiveCode,
                        label = { Text("6 位验证码") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmLanReceive) {
                    Text("接收")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissReceiveDialog) {
                    Text("取消")
                }
            },
        )
    }

    uiState.pendingBluetoothPackage?.let { pending ->
        AlertDialog(
            onDismissRequest = { viewModel.confirmBluetoothReceive(false) },
            title = { Text("接收楼栋分享（蓝牙）") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "收到文件：${pending.meta.fileName}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = uiState.receiveCode,
                        onValueChange = viewModel::updateReceiveCode,
                        label = { Text("6 位验证码") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmBluetoothReceive(true) }) {
                    Text("接收")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.confirmBluetoothReceive(false) }) {
                    Text("拒绝")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("临时组网") },
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
            Text(
                text = "与同事创建或加入临时组网，通过局域网/蓝牙分享楼栋数据。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!bluetoothGranted) {
                Text(
                    text = "请授予蓝牙权限以使用备用通道",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (activeGroup == null) {
                JoinGroupSection(
                    uiState = uiState,
                    viewModel = viewModel,
                )
            } else {
                ActiveGroupSection(
                    uiState = uiState,
                    localBuildings = localBuildings,
                    viewModel = viewModel,
                )
            }

            uiState.statusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            uiState.errorMessage?.let { message ->
                Text(text = message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun JoinGroupSection(
    uiState: GroupUiState,
    viewModel: GroupViewModel,
) {
    Button(onClick = viewModel::createGroup, modifier = Modifier.fillMaxWidth()) {
        Text("创建组网")
    }
    OutlinedButton(
        onClick = viewModel::scanLanGroups,
        enabled = !uiState.isScanning,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (uiState.isScanning) {
            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
        }
        Text(if (uiState.isScanning) "扫描中…" else "扫描局域网")
    }
    uiState.discoveredGroups.forEach { discovered ->
        val selected = uiState.selectedPeer == discovered
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.selectDiscoveredGroup(discovered) },
            colors = CardDefaults.cardColors(
                containerColor = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = discovered.groupInfo.groupName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "发起人：${discovered.groupInfo.hostDeviceName}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    Text(text = "蓝牙加入（已配对设备）", style = MaterialTheme.typography.titleSmall)
    OutlinedButton(onClick = viewModel::refreshPairedDevices, modifier = Modifier.fillMaxWidth()) {
        Text("刷新已配对设备")
    }
    if (uiState.pairedDevices.isEmpty()) {
        Text(
            text = "暂无已配对蓝牙设备",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    uiState.pairedDevices.forEach { device ->
        val selected = uiState.selectedBluetoothDevice == device
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.selectBluetoothDevice(device) },
            colors = CardDefaults.cardColors(
                containerColor = if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = device.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    OutlinedTextField(
        value = uiState.joinCode,
        onValueChange = viewModel::updateJoinCode,
        label = { Text("6 位验证码") },
        modifier = Modifier.fillMaxWidth(),
    )
    Button(onClick = viewModel::joinGroup, modifier = Modifier.fillMaxWidth()) {
        Text("加入组网")
    }
}

@Composable
private fun ActiveGroupSection(
    uiState: GroupUiState,
    localBuildings: List<Building>,
    viewModel: GroupViewModel,
) {
    val activeGroup = uiState.activeGroup ?: return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "验证码：${activeGroup.verifyCode}", style = MaterialTheme.typography.titleLarge)
            Text(text = "发起人：${activeGroup.hostDeviceName}", style = MaterialTheme.typography.bodySmall)
            if (uiState.isLanBeaconActive) {
                Text(
                    text = "局域网广播中",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (uiState.isBluetoothListenerActive) {
                Text(
                    text = "蓝牙监听中",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }

    Text(text = "在线管家", style = MaterialTheme.typography.titleSmall)
    uiState.members.forEach { member ->
        Text(text = member.memberDeviceName, style = MaterialTheme.typography.bodyMedium)
    }

    Text(text = "接收楼栋分享", style = MaterialTheme.typography.titleSmall)
    OutlinedButton(
        onClick = viewModel::scanShareOffers,
        enabled = !uiState.isScanningShares,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (uiState.isScanningShares) {
            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
        }
        Text(if (uiState.isScanningShares) "扫描中…" else "扫描局域网分享")
    }
    uiState.shareOffers.forEach { offer ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.openReceiveDialog(offer) },
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = parseSenderName(offer.manifestJson), style = MaterialTheme.typography.titleSmall)
                Text(
                    text = parseBuildingSummary(offer.manifestJson),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    uiState.activeShare?.let { share ->
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "正在分享：${share.buildingName}", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "验证码：${share.verifyCode}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                OutlinedButton(
                    onClick = viewModel::stopLanShare,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text("停止局域网分享")
                }
            }
        }
    }

    Text(text = "发送我的楼栋", style = MaterialTheme.typography.titleSmall)
    if (localBuildings.isEmpty()) {
        Text(
            text = "暂无本地楼栋可分享",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    localBuildings.forEach { building ->
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = building.name, style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { viewModel.startLanShare(building) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("局域网")
                    }
                    OutlinedButton(
                        onClick = { viewModel.startBluetoothShare(building) },
                        enabled = uiState.selectedBluetoothDevice != null,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("蓝牙")
                    }
                }
            }
        }
    }

    Text(text = "蓝牙发送目标", style = MaterialTheme.typography.titleSmall)
    OutlinedButton(onClick = viewModel::refreshPairedDevices, modifier = Modifier.fillMaxWidth()) {
        Text("刷新已配对设备")
    }
    uiState.pairedDevices.forEach { device ->
        val selected = uiState.selectedBluetoothDevice == device
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.selectBluetoothDevice(device) },
            colors = CardDefaults.cardColors(
                containerColor = if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = device.displayName, style = MaterialTheme.typography.titleSmall)
            }
        }
    }

    if (activeGroup.hostDeviceName == uiState.deviceName) {
        Button(onClick = viewModel::dissolveGroup, modifier = Modifier.fillMaxWidth()) {
            Text("解散组网")
        }
    } else {
        OutlinedButton(onClick = viewModel::leaveGroup, modifier = Modifier.fillMaxWidth()) {
            Text("退出组网")
        }
    }
}

private fun parseSenderName(manifestJson: String): String =
    runCatching { JSONObject(manifestJson).getString("sender_device_name") }
        .getOrDefault("未知设备")

private fun parseBuildingSummary(manifestJson: String): String =
    runCatching {
        val array = JSONObject(manifestJson).getJSONArray("buildings")
        if (array.length() == 0) return "无楼栋信息"
        val first = array.getJSONObject(0)
        val count = array.length()
        if (count == 1) {
            "${first.getString("name")} · ${first.getInt("room_count")} 间"
        } else {
            "${first.getString("name")} 等 $count 栋"
        }
    }.getOrDefault("楼栋分享")
