package com.guanjiazhushou.assistant.data.lan

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class LanShareManifest(
    val deviceName: String,
    val verifyCode: String,
    val packageSizeBytes: Long,
    val checksumSha256: String,
)

data class DiscoveredLanPeer(
    val serviceName: String,
    val hostAddress: String,
    val port: Int,
)

@Singleton
class LanTransferClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    private val nsdManager: NsdManager? =
        context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private val registrationListeners = mutableMapOf<String, NsdManager.RegistrationListener>()
    private val discoveryMutex = Mutex()

    suspend fun fetchGroupInfo(host: String, port: Int): LanGroupInfo {
        val request = Request.Builder()
            .url("http://$host:$port/group/info")
            .get()
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Failed to fetch group info: ${response.code}" }
            val body = response.body?.string().orEmpty()
            val json = JSONObject(body)
            return LanGroupInfo(
                groupName = json.getString("group_name"),
                hostDeviceName = json.getString("host_device_name"),
                verifyCode = json.getString("verify_code"),
            )
        }
    }

    suspend fun requestGroupJoin(
        host: String,
        port: Int,
        memberDeviceName: String,
        verifyCode: String,
    ): Boolean {
        val payload = JSONObject()
            .put("member_device_name", memberDeviceName)
            .put("verify_code", verifyCode)
            .toString()
        val request = Request.Builder()
            .url("http://$host:$port/group/join")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return false
            val body = response.body?.string().orEmpty()
            return JSONObject(body).optBoolean("accepted", false)
        }
    }

    suspend fun fetchManifestOrNull(host: String, port: Int): String? =
        runCatching {
            val request = Request.Builder()
                .url("http://$host:$port/share/manifest")
                .get()
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                response.body?.string()?.takeIf { it.isNotBlank() }
            }
        }.getOrNull()

    suspend fun fetchManifest(host: String, port: Int): String {
        val request = Request.Builder()
            .url("http://$host:$port/share/manifest")
            .get()
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Failed to fetch manifest: ${response.code}" }
            return response.body?.string().orEmpty()
        }
    }

    suspend fun verifyCode(host: String, port: Int, code: String): Boolean {
        val request = Request.Builder()
            .url("http://$host:$port/share/verify?code=$code")
            .get()
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            return response.isSuccessful
        }
    }

    suspend fun downloadPackage(host: String, port: Int, outputFile: File) {
        val request = Request.Builder()
            .url("http://$host:$port/share/download")
            .get()
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Failed to download package: ${response.code}" }
            val body = response.body ?: error("Empty response body")
            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { output ->
                body.byteStream().copyTo(output)
            }
        }
    }

    suspend fun discoverPeers(
        serviceType: String = SERVICE_TYPE,
        timeoutMs: Long = DISCOVERY_TIMEOUT_MS,
    ): List<DiscoveredLanPeer> = discoveryMutex.withLock {
        suspendCancellableCoroutine { continuation ->
            val peers = mutableListOf<DiscoveredLanPeer>()
            val manager = nsdManager
            if (manager == null) {
                continuation.resume(emptyList())
                return@suspendCancellableCoroutine
            }

            val finished = AtomicBoolean(false)
            fun finish(result: List<DiscoveredLanPeer>) {
                if (finished.compareAndSet(false, true) && continuation.isActive) {
                    continuation.resume(result)
                }
            }

            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) = Unit

                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    manager.resolveService(
                        serviceInfo,
                        object : NsdManager.ResolveListener {
                            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit

                            override fun onServiceResolved(resolved: NsdServiceInfo) {
                                val host = resolved.host?.hostAddress ?: return
                                peers.add(
                                    DiscoveredLanPeer(
                                        serviceName = resolved.serviceName,
                                        hostAddress = host,
                                        port = resolved.port,
                                    ),
                                )
                            }
                        },
                    )
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit

                override fun onDiscoveryStopped(serviceType: String) {
                    finish(peers.toList())
                }

                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    finish(emptyList())
                }

                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    finish(peers.toList())
                }
            }

            val started = runCatching {
                manager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
            }
            if (started.isFailure) {
                finish(emptyList())
                return@suspendCancellableCoroutine
            }

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                runCatching { manager.stopServiceDiscovery(listener) }
                finish(peers.toList())
            }, timeoutMs)

            continuation.invokeOnCancellation {
                runCatching { manager.stopServiceDiscovery(listener) }
            }
        }
    }

    fun registerService(
        serviceName: String,
        port: Int,
        serviceType: String = SERVICE_TYPE,
        onRegistered: (() -> Unit)? = null,
        onFailed: ((Int) -> Unit)? = null,
    ) {
        val manager = nsdManager ?: return
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName
            this.serviceType = serviceType
            setPort(port)
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                onRegistered?.invoke()
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                registrationListeners.remove(serviceName)
                onFailed?.invoke(errorCode)
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                registrationListeners.remove(serviceName)
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
        }
        registrationListeners[serviceName] = listener
        manager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    fun unregisterService(serviceName: String) {
        val manager = nsdManager ?: return
        val listener = registrationListeners.remove(serviceName) ?: return
        runCatching { manager.unregisterService(listener) }
    }

    companion object {
        const val SERVICE_TYPE = "_guanjia-pmex._tcp."
        const val DISCOVERY_TIMEOUT_MS = 5_000L
    }
}

@Singleton
class LanHttpClientProvider @Inject constructor() {
    fun provide(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.MINUTES)
        .writeTimeout(10, TimeUnit.MINUTES)
        .build()
}
