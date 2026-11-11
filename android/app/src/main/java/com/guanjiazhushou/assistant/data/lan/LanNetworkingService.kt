package com.guanjiazhushou.assistant.data.lan

import android.content.Context
import com.guanjiazhushou.assistant.data.local.dao.ShareGroupDao
import com.guanjiazhushou.assistant.data.local.entity.ShareGroupMemberEntity
import com.guanjiazhushou.assistant.domain.model.ShareGroup
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanNetworkingService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lanTransferClient: LanTransferClient,
    private val shareGroupDao: ShareGroupDao,
) {
    private val httpServer = LanHttpServer(LanHttpServer.DEFAULT_PORT)
    private var registeredServiceName: String? = null

    val isBeaconActive: Boolean
        get() = httpServer.groupBeacon != null

    val isShareActive: Boolean
        get() = httpServer.shareSession != null

    fun startGroupBeacon(group: ShareGroup) {
        ensureServerRunning()
        httpServer.groupBeacon = GroupBeaconPayload(
            groupName = group.name,
            hostDeviceName = group.hostDeviceName,
            verifyCode = group.verifyCode,
        )
        httpServer.onGroupJoinRequest = { memberDeviceName, verifyCode ->
            handleGroupJoinRequest(memberDeviceName, verifyCode)
        }
        registerNsdService(group.hostDeviceName, group.verifyCode)
    }

    fun startBuildingShare(
        pmexFile: File,
        manifestJson: String,
        verifyCode: String,
        deviceName: String,
    ) {
        ensureServerRunning()
        httpServer.shareSession = ShareSessionPayload(
            pmexFile = pmexFile,
            manifestJson = manifestJson,
            verifyCode = verifyCode,
        )
        registerNsdService(deviceName, verifyCode)
    }

    fun stopBuildingShare() {
        httpServer.shareSession = null
        val beacon = httpServer.groupBeacon
        if (beacon != null) {
            registerNsdService(beacon.hostDeviceName, beacon.verifyCode)
        } else if (!httpServer.isRunning) {
            unregisterNsdService()
        }
    }

    fun stopAll() {
        runCatching { unregisterNsdService() }
        httpServer.stopServer()
        registeredServiceName = null
    }

    suspend fun discoverNearbyGroups(): List<DiscoveredLanGroup> = withContext(Dispatchers.IO) {
        val peers = lanTransferClient.discoverPeers()
        peers.mapNotNull { peer ->
            val info = runCatching { lanTransferClient.fetchGroupInfo(peer.hostAddress, peer.port) }
                .getOrNull()
                ?: return@mapNotNull null
            DiscoveredLanGroup(peer = peer, groupInfo = info)
        }
    }

    suspend fun discoverShareOffers(): List<LanShareOffer> = withContext(Dispatchers.IO) {
        val peers = runCatching { lanTransferClient.discoverPeers() }.getOrDefault(emptyList())
        peers.mapNotNull { peer ->
            val manifest = runCatching {
                lanTransferClient.fetchManifestOrNull(peer.hostAddress, peer.port)
            }.getOrNull() ?: return@mapNotNull null
            LanShareOffer(peer = peer, manifestJson = manifest)
        }
    }

    suspend fun requestJoinGroup(
        peer: DiscoveredLanPeer,
        verifyCode: String,
        memberDeviceName: String,
    ): LanGroupInfo? = withContext(Dispatchers.IO) {
        val remoteInfo = runCatching {
            lanTransferClient.fetchGroupInfo(peer.hostAddress, peer.port)
        }.getOrNull() ?: return@withContext null
        if (remoteInfo.verifyCode != verifyCode) return@withContext null
        val accepted = lanTransferClient.requestGroupJoin(
            host = peer.hostAddress,
            port = peer.port,
            memberDeviceName = memberDeviceName,
            verifyCode = verifyCode,
        )
        if (accepted) remoteInfo else null
    }

    suspend fun verifyAndDownloadShare(
        peer: DiscoveredLanPeer,
        verifyCode: String,
        outputFile: File,
    ): String? = withContext(Dispatchers.IO) {
        val verified = lanTransferClient.verifyCode(peer.hostAddress, peer.port, verifyCode)
        if (!verified) return@withContext null
        val manifest = lanTransferClient.fetchManifest(peer.hostAddress, peer.port)
        lanTransferClient.downloadPackage(peer.hostAddress, peer.port, outputFile)
        manifest
    }

    private fun ensureServerRunning() {
        if (!httpServer.isRunning) {
            httpServer.startServer()
        }
    }

    private fun handleGroupJoinRequest(memberDeviceName: String, verifyCode: String): Boolean =
        runBlocking(Dispatchers.IO) {
            val activeGroup = shareGroupDao.getActiveGroup() ?: return@runBlocking false
            if (activeGroup.verifyCode != verifyCode) return@runBlocking false
            shareGroupDao.insertMember(
                ShareGroupMemberEntity(
                    groupId = activeGroup.id,
                    memberDeviceName = memberDeviceName,
                    joinedAt = System.currentTimeMillis(),
                ),
            )
            true
        }

    private fun registerNsdService(deviceName: String, verifyCode: String) {
        unregisterNsdService()
        val sanitized = deviceName
            .replace(Regex("[^a-zA-Z0-9_-]"), "")
            .take(12)
            .ifBlank { "guanjia" }
        val serviceName = "GJ-$sanitized-${verifyCode.takeLast(2)}"
        registeredServiceName = serviceName
        lanTransferClient.registerService(
            serviceName = serviceName,
            port = LanHttpServer.DEFAULT_PORT,
        )
    }

    private fun unregisterNsdService() {
        registeredServiceName?.let {
            lanTransferClient.unregisterService(it)
        }
        registeredServiceName = null
    }

    fun cacheDir(): File = File(context.cacheDir, "lan_share").apply { mkdirs() }
}
