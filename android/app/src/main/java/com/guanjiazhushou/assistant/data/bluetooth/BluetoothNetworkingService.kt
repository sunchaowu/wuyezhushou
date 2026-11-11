package com.guanjiazhushou.assistant.data.bluetooth

import android.content.Context
import com.guanjiazhushou.assistant.data.lan.LanGroupInfo
import com.guanjiazhushou.assistant.data.local.dao.ShareGroupDao
import com.guanjiazhushou.assistant.data.local.entity.ShareGroupMemberEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class PendingBluetoothPackage(
    val meta: BluetoothTransferMeta,
)

data class ReceivedBluetoothPackage(
    val meta: BluetoothTransferMeta,
    val file: File,
)

@Singleton
class BluetoothNetworkingService @Inject constructor(
    @ApplicationContext @Suppress("unused") private val context: Context,
    private val bluetoothTransferService: BluetoothTransferService,
    private val shareGroupDao: ShareGroupDao,
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var packageDecision = CompletableDeferred<Boolean>()

    private val _pendingPackage = MutableStateFlow<PendingBluetoothPackage?>(null)
    val pendingPackage: StateFlow<PendingBluetoothPackage?> = _pendingPackage.asStateFlow()

    private val _receivedPackage = MutableStateFlow<ReceivedBluetoothPackage?>(null)
    val receivedPackage: StateFlow<ReceivedBluetoothPackage?> = _receivedPackage.asStateFlow()

    fun getPairedDevices(): List<BluetoothDeviceInfo> =
        bluetoothTransferService.getPairedDevices()

    fun isBluetoothReady(): Boolean = bluetoothTransferService.isBluetoothAvailable()

    fun startGroupAndPackageListener() {
        resetPackageDecision()
        bluetoothTransferService.startListening(
            scope = serviceScope,
            onGroupJoin = { memberDeviceName, verifyCode ->
                handleGroupJoin(memberDeviceName, verifyCode)
            },
            onPackage = { meta ->
                _pendingPackage.value = PendingBluetoothPackage(meta)
                packageDecision.await()
            },
            onPackageReceived = { meta, file ->
                _pendingPackage.value = null
                _receivedPackage.value = ReceivedBluetoothPackage(meta, file)
            },
        )
    }

    fun startPackageListener() {
        resetPackageDecision()
        bluetoothTransferService.startListening(
            scope = serviceScope,
            onGroupJoin = { _, _ -> BluetoothGroupJoinResult(accepted = false) },
            onPackage = { meta ->
                _pendingPackage.value = PendingBluetoothPackage(meta)
                packageDecision.await()
            },
            onPackageReceived = { meta, file ->
                _pendingPackage.value = null
                _receivedPackage.value = ReceivedBluetoothPackage(meta, file)
            },
        )
    }

    fun respondToIncomingPackage(accept: Boolean) {
        if (!packageDecision.isCompleted) {
            packageDecision.complete(accept)
        }
        if (!accept) {
            _pendingPackage.value = null
        }
    }

    fun clearReceivedPackage() {
        _receivedPackage.value = null
    }

    fun stopListening() {
        if (!packageDecision.isCompleted) {
            packageDecision.complete(false)
        }
        bluetoothTransferService.stopListening()
        _pendingPackage.value = null
    }

    suspend fun joinGroupViaBluetooth(
        deviceAddress: String,
        verifyCode: String,
        memberDeviceName: String,
    ): LanGroupInfo? = withContext(Dispatchers.IO) {
        val device = bluetoothTransferService.getDevice(deviceAddress) ?: return@withContext null
        val result = bluetoothTransferService.sendGroupJoinRequest(
            device = device,
            memberDeviceName = memberDeviceName,
            verifyCode = verifyCode,
        ) ?: return@withContext null
        if (!result.accepted) return@withContext null
        LanGroupInfo(
            groupName = result.groupName,
            hostDeviceName = result.hostDeviceName,
            verifyCode = result.verifyCode,
        )
    }

    suspend fun sendPackage(
        deviceAddress: String,
        packageFile: File,
        meta: BluetoothTransferMeta,
    ): Boolean = withContext(Dispatchers.IO) {
        val device = bluetoothTransferService.getDevice(deviceAddress) ?: return@withContext false
        bluetoothTransferService.sendPackage(device, packageFile, meta)
    }

    fun cacheDir(): File = bluetoothTransferService.receiveCacheDir()

    private fun resetPackageDecision() {
        if (!packageDecision.isCompleted) {
            packageDecision.complete(false)
        }
        packageDecision = CompletableDeferred()
    }

    private suspend fun handleGroupJoin(
        memberDeviceName: String,
        verifyCode: String,
    ): BluetoothGroupJoinResult = withContext(Dispatchers.IO) {
        val activeGroup = shareGroupDao.getActiveGroup()
            ?: return@withContext BluetoothGroupJoinResult(accepted = false)
        if (activeGroup.verifyCode != verifyCode) {
            return@withContext BluetoothGroupJoinResult(accepted = false)
        }
        shareGroupDao.insertMember(
            ShareGroupMemberEntity(
                groupId = activeGroup.id,
                memberDeviceName = memberDeviceName,
                joinedAt = System.currentTimeMillis(),
            ),
        )
        BluetoothGroupJoinResult(
            accepted = true,
            groupName = activeGroup.name,
            hostDeviceName = activeGroup.hostDeviceName,
            verifyCode = activeGroup.verifyCode,
        )
    }
}
