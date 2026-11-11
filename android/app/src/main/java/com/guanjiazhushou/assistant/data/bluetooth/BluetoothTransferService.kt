package com.guanjiazhushou.assistant.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Build
import com.guanjiazhushou.assistant.util.hasBluetoothPermissions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

data class BluetoothTransferMeta(
    val fileName: String,
    val fileSizeBytes: Long,
    val checksumSha256: String,
    val verifyCode: String,
)

@Singleton
class BluetoothTransferService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val adapter: BluetoothAdapter? = resolveAdapter()

    private var serverSocket: BluetoothServerSocket? = null
    private val listening = AtomicBoolean(false)
    private var listenerJob: Job? = null

    fun isBluetoothAvailable(): Boolean = adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDeviceInfo> {
        if (!hasBluetoothPermissions(context)) {
            return emptyList()
        }
        val bonded = adapter?.bondedDevices.orEmpty()
        return bonded.map { device ->
            BluetoothDeviceInfo(
                address = device.address,
                displayName = device.name?.takeIf { it.isNotBlank() } ?: device.address,
            )
        }.sortedBy { it.displayName }
    }

    fun getDevice(address: String): BluetoothDevice? = adapter?.getRemoteDevice(address)

    fun startListening(
        scope: CoroutineScope,
        onGroupJoin: suspend (memberDeviceName: String, verifyCode: String) -> BluetoothGroupJoinResult,
        onPackage: suspend (BluetoothTransferMeta) -> Boolean,
        onPackageReceived: suspend (BluetoothTransferMeta, File) -> Unit = { _, _ -> },
    ) {
        if (!listening.compareAndSet(false, true)) return
        listenerJob = scope.launch(Dispatchers.IO) {
            val server = openServerSocket() ?: run {
                listening.set(false)
                return@launch
            }
            serverSocket = server
            try {
                while (listening.get() && scope.isActive) {
                    val socket = runCatching { server.accept() }.getOrNull() ?: continue
                    launch {
                        handleIncomingConnection(socket, onGroupJoin, onPackage, onPackageReceived)
                    }
                }
            } finally {
                runCatching { server.close() }
                serverSocket = null
                listening.set(false)
            }
        }
    }

    fun stopListening() {
        listening.set(false)
        listenerJob?.cancel()
        listenerJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    suspend fun sendGroupJoinRequest(
        device: BluetoothDevice,
        memberDeviceName: String,
        verifyCode: String,
    ): BluetoothGroupJoinResult? = withContext(Dispatchers.IO) {
        val socket = openClientSocket(device) ?: return@withContext null
        try {
            val output = DataOutputStream(socket.outputStream)
            val input = DataInputStream(socket.inputStream)
            output.writeUTF(MSG_GROUP_JOIN)
            output.writeUTF(memberDeviceName)
            output.writeUTF(verifyCode)
            output.flush()
            val accepted = input.readBoolean()
            if (!accepted) {
                return@withContext BluetoothGroupJoinResult(accepted = false)
            }
            BluetoothGroupJoinResult(
                accepted = true,
                groupName = input.readUTF(),
                hostDeviceName = input.readUTF(),
                verifyCode = input.readUTF(),
            )
        } finally {
            socket.close()
        }
    }

    suspend fun sendPackage(
        device: BluetoothDevice,
        packageFile: File,
        meta: BluetoothTransferMeta,
    ): Boolean = withContext(Dispatchers.IO) {
        val socket = openClientSocket(device) ?: return@withContext false
        try {
            val output = DataOutputStream(socket.outputStream)
            val input = DataInputStream(socket.inputStream)
            output.writeUTF(MSG_PACKAGE)
            writeMeta(output, meta)
            val ack = input.readBoolean()
            if (!ack) return@withContext false
            streamFile(output, packageFile)
            true
        } finally {
            socket.close()
        }
    }

    private suspend fun handleIncomingConnection(
        socket: BluetoothSocket,
        onGroupJoin: suspend (String, String) -> BluetoothGroupJoinResult,
        onPackage: suspend (BluetoothTransferMeta) -> Boolean,
        onPackageReceived: suspend (BluetoothTransferMeta, File) -> Unit,
    ) = withContext(Dispatchers.IO) {
        try {
            val input = DataInputStream(socket.inputStream)
            val output = DataOutputStream(socket.outputStream)
            when (input.readUTF()) {
                MSG_GROUP_JOIN -> {
                    val member = input.readUTF()
                    val code = input.readUTF()
                    val result = onGroupJoin(member, code)
                    output.writeBoolean(result.accepted)
                    if (result.accepted) {
                        output.writeUTF(result.groupName)
                        output.writeUTF(result.hostDeviceName)
                        output.writeUTF(result.verifyCode)
                    }
                    output.flush()
                }
                MSG_PACKAGE -> {
                    val meta = readMeta(input)
                    val accepted = onPackage(meta)
                    output.writeBoolean(accepted)
                    output.flush()
                    if (accepted) {
                        val file = buildReceiveFile(meta.fileName)
                        readFile(input, file, meta.fileSizeBytes)
                        onPackageReceived(meta, file)
                    }
                }
            }
        } finally {
            socket.close()
        }
    }

    private fun buildReceiveFile(fileName: String): File {
        val dir = File(context.cacheDir, "bt_share").apply { mkdirs() }
        return File(dir, fileName)
    }

    fun receiveCacheDir(): File = File(context.cacheDir, "bt_share").apply { mkdirs() }

    private fun openClientSocket(device: BluetoothDevice): BluetoothSocket? {
        runCatching {
            return device.createRfcommSocketToServiceRecord(SPP_UUID).apply { connect() }
        }
        @Suppress("DEPRECATION")
        return runCatching {
            val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            (method.invoke(device, 1) as BluetoothSocket).apply { connect() }
        }.getOrNull()
    }

    private fun openServerSocket(): BluetoothServerSocket? =
        runCatching {
            adapter?.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SPP_UUID)
        }.getOrNull()

    private fun resolveAdapter(): BluetoothAdapter? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            manager?.adapter
        } else {
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()
        }

    private fun writeMeta(output: DataOutputStream, meta: BluetoothTransferMeta) {
        output.writeUTF(meta.fileName)
        output.writeLong(meta.fileSizeBytes)
        output.writeUTF(meta.checksumSha256)
        output.writeUTF(meta.verifyCode)
        output.flush()
    }

    private fun readMeta(input: DataInputStream): BluetoothTransferMeta =
        BluetoothTransferMeta(
            fileName = input.readUTF(),
            fileSizeBytes = input.readLong(),
            checksumSha256 = input.readUTF(),
            verifyCode = input.readUTF(),
        )

    private fun streamFile(output: DataOutputStream, file: File) {
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read = input.read(buffer)
            while (read > 0) {
                output.write(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        output.flush()
    }

    private fun readFile(input: DataInputStream, outputFile: File, expectedSize: Long) {
        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { output ->
            var remaining = expectedSize
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (remaining > 0) {
                val toRead = minOf(buffer.size.toLong(), remaining).toInt()
                val read = input.read(buffer, 0, toRead)
                if (read <= 0) break
                output.write(buffer, 0, read)
                remaining -= read
            }
        }
    }

    companion object {
        private const val SERVICE_NAME = "GuanjiaAssistantPmex"
        private const val MSG_GROUP_JOIN = "group_join"
        private const val MSG_PACKAGE = "package"
        private val SPP_UUID: UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
