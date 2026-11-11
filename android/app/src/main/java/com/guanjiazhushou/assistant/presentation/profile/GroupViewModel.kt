package com.guanjiazhushou.assistant.presentation.profile

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guanjiazhushou.assistant.data.bluetooth.BluetoothDeviceInfo
import com.guanjiazhushou.assistant.data.bluetooth.BluetoothNetworkingService
import com.guanjiazhushou.assistant.data.bluetooth.BluetoothTransferMeta
import com.guanjiazhushou.assistant.data.bluetooth.PendingBluetoothPackage
import com.guanjiazhushou.assistant.data.lan.DiscoveredLanGroup
import com.guanjiazhushou.assistant.data.lan.LanNetworkingService
import com.guanjiazhushou.assistant.data.lan.LanShareOffer
import com.guanjiazhushou.assistant.domain.model.Building
import com.guanjiazhushou.assistant.domain.model.ShareGroup
import com.guanjiazhushou.assistant.domain.model.ShareGroupMember
import com.guanjiazhushou.assistant.domain.model.SourceType
import com.guanjiazhushou.assistant.domain.repository.BuildingRepository
import com.guanjiazhushou.assistant.domain.repository.ShareRepository
import com.guanjiazhushou.assistant.domain.usecase.CreateShareGroupUseCase
import com.guanjiazhushou.assistant.domain.usecase.DissolveShareGroupUseCase
import com.guanjiazhushou.assistant.domain.usecase.ExportPmexUseCase
import com.guanjiazhushou.assistant.domain.usecase.ImportPmexUseCase
import com.guanjiazhushou.assistant.domain.usecase.LeaveShareGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import kotlin.random.Random

data class ActiveLanShare(
    val buildingName: String,
    val verifyCode: String,
)

data class GroupUiState(
    val activeGroup: ShareGroup? = null,
    val members: List<ShareGroupMember> = emptyList(),
    val joinCode: String = "",
    val discoveredGroups: List<DiscoveredLanGroup> = emptyList(),
    val selectedPeer: DiscoveredLanGroup? = null,
    val pairedDevices: List<BluetoothDeviceInfo> = emptyList(),
    val selectedBluetoothDevice: BluetoothDeviceInfo? = null,
    val isScanning: Boolean = false,
    val isLanBeaconActive: Boolean = false,
    val isBluetoothListenerActive: Boolean = false,
    val isScanningShares: Boolean = false,
    val shareOffers: List<LanShareOffer> = emptyList(),
    val activeShare: ActiveLanShare? = null,
    val receiveTarget: LanShareOffer? = null,
    val pendingBluetoothPackage: PendingBluetoothPackage? = null,
    val receiveCode: String = "",
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val deviceName: String = Build.MODEL,
)

@HiltViewModel
class GroupViewModel @Inject constructor(
    buildingRepository: BuildingRepository,
    private val shareRepository: ShareRepository,
    private val createShareGroupUseCase: CreateShareGroupUseCase,
    private val dissolveShareGroupUseCase: DissolveShareGroupUseCase,
    private val leaveShareGroupUseCase: LeaveShareGroupUseCase,
    private val exportPmexUseCase: ExportPmexUseCase,
    private val importPmexUseCase: ImportPmexUseCase,
    private val lanNetworkingService: LanNetworkingService,
    private val bluetoothNetworkingService: BluetoothNetworkingService,
) : ViewModel() {

    val localBuildings: StateFlow<List<Building>> = buildingRepository.observeAll()
        .map { list -> list.filter { it.sourceType == SourceType.LOCAL } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            shareRepository.observeGroups().collect { groups ->
                val group = groups.firstOrNull { it.status == "active" }
                _uiState.update { it.copy(activeGroup = group) }
                if (group != null) {
                    val members = shareRepository.getMembers(group.id)
                    _uiState.update { it.copy(members = members) }
                    if (group.hostDeviceName == _uiState.value.deviceName) {
                        withContext(Dispatchers.IO) {
                            lanNetworkingService.startGroupBeacon(group)
                            bluetoothNetworkingService.startGroupAndPackageListener()
                        }
                        _uiState.update {
                            it.copy(
                                isLanBeaconActive = true,
                                isBluetoothListenerActive = true,
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            members = emptyList(),
                            isLanBeaconActive = false,
                            isBluetoothListenerActive = false,
                            activeShare = null,
                        )
                    }
                    withContext(Dispatchers.IO) {
                        lanNetworkingService.stopAll()
                        bluetoothNetworkingService.stopListening()
                    }
                }
            }
        }
        viewModelScope.launch {
            bluetoothNetworkingService.pendingPackage.collect { pending ->
                _uiState.update {
                    it.copy(
                        pendingBluetoothPackage = pending,
                        receiveCode = pending?.meta?.verifyCode.orEmpty(),
                    )
                }
            }
        }
        viewModelScope.launch {
            bluetoothNetworkingService.receivedPackage.collect { received ->
                received ?: return@collect
                importBluetoothPackage(received.file, received.meta.verifyCode)
                bluetoothNetworkingService.clearReceivedPackage()
            }
        }
    }

    fun refreshPairedDevices() {
        _uiState.update { it.copy(pairedDevices = bluetoothNetworkingService.getPairedDevices()) }
    }

    fun updateJoinCode(value: String) = _uiState.update { it.copy(joinCode = value) }

    fun updateReceiveCode(value: String) = _uiState.update { it.copy(receiveCode = value) }

    fun selectDiscoveredGroup(group: DiscoveredLanGroup) {
        _uiState.update {
            it.copy(
                selectedPeer = group,
                selectedBluetoothDevice = null,
                joinCode = group.groupInfo.verifyCode,
                errorMessage = null,
            )
        }
    }

    fun selectBluetoothDevice(device: BluetoothDeviceInfo) {
        _uiState.update {
            it.copy(
                selectedBluetoothDevice = device,
                selectedPeer = null,
                errorMessage = null,
            )
        }
    }

    fun scanLanGroups() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(isScanning = true, statusMessage = "正在扫描局域网…", errorMessage = null)
            }
            val discovered = lanNetworkingService.discoverNearbyGroups()
            _uiState.update {
                it.copy(
                    isScanning = false,
                    discoveredGroups = discovered,
                    statusMessage = if (discovered.isEmpty()) {
                        "未发现局域网组网，可尝试蓝牙加入（需先配对）"
                    } else {
                        "发现 ${discovered.size} 个局域网组网"
                    },
                )
            }
        }
    }

    fun createGroup() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = createShareGroupUseCase("临时组网", _uiState.value.deviceName)
                val group = shareRepository.getById(result.groupId)
                if (group != null) {
                    lanNetworkingService.startGroupBeacon(group)
                    bluetoothNetworkingService.startGroupAndPackageListener()
                    _uiState.update {
                        it.copy(
                            isLanBeaconActive = true,
                            isBluetoothListenerActive = true,
                            errorMessage = null,
                            statusMessage = "组网已创建，局域网与蓝牙监听已开启",
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun joinGroup() {
        val code = _uiState.value.joinCode.trim()
        if (code.length != 6) {
            _uiState.update { it.copy(errorMessage = "请输入 6 位验证码") }
            return
        }
        val bluetoothDevice = _uiState.value.selectedBluetoothDevice
        if (bluetoothDevice != null) {
            joinGroupViaBluetooth(bluetoothDevice, code)
            return
        }
        val peer = _uiState.value.selectedPeer
            ?: _uiState.value.discoveredGroups.firstOrNull { it.groupInfo.verifyCode == code }
        if (peer == null) {
            _uiState.update {
                it.copy(errorMessage = "请先扫描局域网或选择已配对的蓝牙设备")
            }
            return
        }
        joinGroupViaLan(peer, code)
    }

    private fun joinGroupViaLan(peer: DiscoveredLanGroup, code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(statusMessage = "正在通过局域网加入…", errorMessage = null) }
            val remoteInfo = lanNetworkingService.requestJoinGroup(
                peer = peer.peer,
                verifyCode = code,
                memberDeviceName = _uiState.value.deviceName,
            )
            if (remoteInfo == null) {
                _uiState.update { it.copy(errorMessage = "局域网加入失败，可尝试蓝牙方式") }
                return@launch
            }
            completeJoin(remoteInfo.groupName, remoteInfo.hostDeviceName, code)
        }
    }

    private fun joinGroupViaBluetooth(device: BluetoothDeviceInfo, code: String) {
        if (!bluetoothNetworkingService.isBluetoothReady()) {
            _uiState.update { it.copy(errorMessage = "请先开启蓝牙并在系统设置中完成配对") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(statusMessage = "正在通过蓝牙加入…", errorMessage = null) }
            val remoteInfo = bluetoothNetworkingService.joinGroupViaBluetooth(
                deviceAddress = device.address,
                verifyCode = code,
                memberDeviceName = _uiState.value.deviceName,
            )
            if (remoteInfo == null) {
                _uiState.update { it.copy(errorMessage = "蓝牙加入失败，请确认验证码与配对状态") }
                return@launch
            }
            completeJoin(remoteInfo.groupName, remoteInfo.hostDeviceName, code)
        }
    }

    private suspend fun completeJoin(groupName: String, hostDeviceName: String, code: String) {
        try {
            shareRepository.joinRemoteGroup(
                name = groupName,
                hostDeviceName = hostDeviceName,
                verifyCode = code,
                memberDeviceName = _uiState.value.deviceName,
            )
            _uiState.update {
                it.copy(
                    joinCode = "",
                    selectedPeer = null,
                    selectedBluetoothDevice = null,
                    discoveredGroups = emptyList(),
                    statusMessage = "已成功加入组网",
                    errorMessage = null,
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = e.message ?: "加入失败") }
        }
    }

    fun dissolveGroup() {
        viewModelScope.launch(Dispatchers.IO) {
            lanNetworkingService.stopAll()
            bluetoothNetworkingService.stopListening()
            _uiState.value.activeGroup?.let { dissolveShareGroupUseCase(it.id) }
            _uiState.update {
                it.copy(
                    isLanBeaconActive = false,
                    isBluetoothListenerActive = false,
                    activeShare = null,
                    statusMessage = "组网已解散",
                )
            }
        }
    }

    fun leaveGroup() {
        viewModelScope.launch(Dispatchers.IO) {
            val group = _uiState.value.activeGroup ?: return@launch
            leaveShareGroupUseCase(group.id, _uiState.value.deviceName)
            if (group.hostDeviceName != _uiState.value.deviceName) {
                lanNetworkingService.stopAll()
            }
            _uiState.update {
                it.copy(
                    activeShare = null,
                    statusMessage = "已退出组网",
                )
            }
        }
    }

    fun scanShareOffers() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(isScanningShares = true, statusMessage = "正在扫描局域网分享…", errorMessage = null)
            }
            try {
                val offers = lanNetworkingService.discoverShareOffers()
                _uiState.update {
                    it.copy(
                        isScanningShares = false,
                        shareOffers = offers,
                        statusMessage = if (offers.isEmpty()) {
                            "未发现局域网分享"
                        } else {
                            "发现 ${offers.size} 个局域网分享"
                        },
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanningShares = false,
                        errorMessage = e.message ?: "扫描失败，请确认已连接同一 Wi-Fi",
                    )
                }
            }
        }
    }

    fun startLanShare(building: Building) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val verifyCode = generateVerifyCode()
                val groupName = shareRepository.getActiveGroup()?.name
                val outputFile = File(
                    lanNetworkingService.cacheDir(),
                    "share_${building.id}_${System.currentTimeMillis()}.pmex",
                )
                val exportResult = exportPmexUseCase(
                    buildingIds = listOf(building.id),
                    senderDeviceName = _uiState.value.deviceName,
                    verifyCode = verifyCode,
                    outputFile = outputFile,
                    groupName = groupName,
                )
                lanNetworkingService.startBuildingShare(
                    pmexFile = File(exportResult.filePath),
                    manifestJson = exportResult.manifest.toManifestJson(),
                    verifyCode = verifyCode,
                    deviceName = _uiState.value.deviceName,
                )
                _uiState.update {
                    it.copy(
                        activeShare = ActiveLanShare(building.name, verifyCode),
                        statusMessage = "「${building.name}」局域网分享中，验证码：$verifyCode",
                        errorMessage = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "分享启动失败") }
            }
        }
    }

    fun startBluetoothShare(building: Building) {
        val device = _uiState.value.selectedBluetoothDevice
        if (device == null) {
            _uiState.update { it.copy(errorMessage = "请先选择已配对的蓝牙设备") }
            return
        }
        if (!bluetoothNetworkingService.isBluetoothReady()) {
            _uiState.update { it.copy(errorMessage = "请先开启蓝牙") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val verifyCode = generateVerifyCode()
                val groupName = shareRepository.getActiveGroup()?.name
                val outputFile = File(
                    bluetoothNetworkingService.cacheDir(),
                    "bt_share_${building.id}_${System.currentTimeMillis()}.pmex",
                )
                val exportResult = exportPmexUseCase(
                    buildingIds = listOf(building.id),
                    senderDeviceName = _uiState.value.deviceName,
                    verifyCode = verifyCode,
                    outputFile = outputFile,
                    groupName = groupName,
                )
                val sent = bluetoothNetworkingService.sendPackage(
                    deviceAddress = device.address,
                    packageFile = File(exportResult.filePath),
                    meta = BluetoothTransferMeta(
                        fileName = outputFile.name,
                        fileSizeBytes = exportResult.fileSizeBytes,
                        checksumSha256 = exportResult.manifest.checksumSha256,
                        verifyCode = verifyCode,
                    ),
                )
                _uiState.update {
                    if (sent) {
                        it.copy(
                            statusMessage = "已通过蓝牙发送「${building.name}」，验证码：$verifyCode",
                            errorMessage = null,
                        )
                    } else {
                        it.copy(errorMessage = "蓝牙发送失败，请确认对方已开启接收且设备已配对")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "蓝牙发送失败") }
            }
        }
    }

    fun stopLanShare() {
        viewModelScope.launch(Dispatchers.IO) {
            lanNetworkingService.stopBuildingShare()
            _uiState.update {
                it.copy(activeShare = null, statusMessage = "已停止局域网分享")
            }
        }
    }

    fun openReceiveDialog(offer: LanShareOffer) {
        val code = runCatching {
            JSONObject(offer.manifestJson).getString("verify_code")
        }.getOrDefault("")
        _uiState.update {
            it.copy(receiveTarget = offer, receiveCode = code, errorMessage = null)
        }
    }

    fun dismissReceiveDialog() = _uiState.update { it.copy(receiveTarget = null) }

    fun confirmLanReceive() {
        val target = _uiState.value.receiveTarget ?: return
        val code = _uiState.value.receiveCode.trim()
        if (code.length != 6) {
            _uiState.update { it.copy(errorMessage = "请输入 6 位验证码") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val outputFile = File(
                    lanNetworkingService.cacheDir(),
                    "receive_${System.currentTimeMillis()}.pmex",
                )
                val manifestJson = lanNetworkingService.verifyAndDownloadShare(
                    peer = target.peer,
                    verifyCode = code,
                    outputFile = outputFile,
                )
                if (manifestJson == null) {
                    _uiState.update { it.copy(errorMessage = "验证码错误或下载失败") }
                    return@launch
                }
                importPackage(outputFile, code)
                _uiState.update { it.copy(receiveTarget = null, receiveCode = "") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "接收失败") }
            }
        }
    }

    fun confirmBluetoothReceive(accept: Boolean) {
        if (!accept) {
            bluetoothNetworkingService.respondToIncomingPackage(false)
            _uiState.update { it.copy(pendingBluetoothPackage = null) }
            return
        }
        val code = _uiState.value.receiveCode.trim()
        if (code.length != 6) {
            _uiState.update { it.copy(errorMessage = "请输入 6 位验证码") }
            return
        }
        bluetoothNetworkingService.respondToIncomingPackage(true)
    }

    private suspend fun importBluetoothPackage(file: File, code: String) {
        try {
            importPackage(file, code)
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = e.message ?: "导入失败") }
        }
    }

    private suspend fun importPackage(file: File, code: String) {
        val result = importPmexUseCase(
            inputFile = file,
            verifyCode = code,
            sourceGroupId = shareRepository.getActiveGroup()?.id,
        )
        _uiState.update {
            it.copy(
                pendingBluetoothPackage = null,
                statusMessage = "已导入 ${result.roomCount} 间房屋（${result.importedBuildingIds.size} 栋）",
                errorMessage = null,
            )
        }
    }

    private fun generateVerifyCode(): String =
        Random.nextInt(0, 1_000_000).toString().padStart(6, '0')
}
