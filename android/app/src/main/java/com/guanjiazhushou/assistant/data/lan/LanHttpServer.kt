package com.guanjiazhushou.assistant.data.lan

import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicBoolean

data class GroupBeaconPayload(
    val groupName: String,
    val hostDeviceName: String,
    val verifyCode: String,
)

data class ShareSessionPayload(
    val pmexFile: File,
    val manifestJson: String,
    val verifyCode: String,
)

class LanHttpServer(
    port: Int = DEFAULT_PORT,
) : NanoHTTPD(port) {
    private val started = AtomicBoolean(false)

    @Volatile
    var groupBeacon: GroupBeaconPayload? = null

    @Volatile
    var shareSession: ShareSessionPayload? = null

    var onGroupJoinRequest: ((memberDeviceName: String, verifyCode: String) -> Boolean)? = null

    fun startServer() {
        if (started.compareAndSet(false, true)) {
            start(SOCKET_READ_TIMEOUT, false)
        }
    }

    val isRunning: Boolean
        get() = started.get()

    fun stopServer() {
        if (started.compareAndSet(true, false)) {
            stop()
        }
        groupBeacon = null
        shareSession = null
        onGroupJoinRequest = null
    }

    override fun serve(session: IHTTPSession): Response =
        when (session.method) {
            Method.GET -> serveGet(session)
            Method.POST -> servePost(session)
            else -> notFound()
        }

    private fun serveGet(session: IHTTPSession): Response =
        when (session.uri) {
            "/group/info" -> serveGroupInfo()
            "/share/manifest" -> serveShareManifest()
            "/share/download" -> serveShareDownload()
            "/share/verify" -> serveShareVerify(session)
            else -> notFound()
        }

    private fun servePost(session: IHTTPSession): Response =
        when (session.uri) {
            "/group/join" -> serveGroupJoin(session)
            else -> notFound()
        }

    private fun serveGroupInfo(): Response {
        val beacon = groupBeacon
            ?: return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "No active group",
            )
        val json = JSONObject()
            .put("group_name", beacon.groupName)
            .put("host_device_name", beacon.hostDeviceName)
            .put("verify_code", beacon.verifyCode)
        return jsonResponse(json.toString())
    }

    private fun serveGroupJoin(session: IHTTPSession): Response {
        val beacon = groupBeacon
            ?: return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "No active group",
            )
        val body = readBody(session)
        val json = runCatching { JSONObject(body) }.getOrNull()
            ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid JSON")
        val memberDeviceName = json.optString("member_device_name")
        val verifyCode = json.optString("verify_code")
        if (memberDeviceName.isBlank() || verifyCode.isBlank()) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing fields")
        }
        val accepted = onGroupJoinRequest?.invoke(memberDeviceName, verifyCode) == true
        val response = JSONObject().put("accepted", accepted)
        return if (accepted) {
            jsonResponse(response.toString())
        } else {
            newFixedLengthResponse(
                Response.Status.FORBIDDEN,
                "application/json; charset=utf-8",
                response.toString(),
            )
        }
    }

    private fun serveShareManifest(): Response {
        val session = shareSession
            ?: return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "No active share",
            )
        return newFixedLengthResponse(
            Response.Status.OK,
            "application/json; charset=utf-8",
            session.manifestJson,
        )
    }

    private fun serveShareDownload(): Response {
        val session = shareSession
            ?: return notFound()
        val file = session.pmexFile
        if (!file.exists()) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Package not found")
        }
        val stream = FileInputStream(file)
        return newFixedLengthResponse(
            Response.Status.OK,
            "application/octet-stream",
            stream,
            file.length(),
        )
    }

    private fun serveShareVerify(session: IHTTPSession): Response {
        val share = shareSession
        val beacon = groupBeacon
        val expectedCode = share?.verifyCode ?: beacon?.verifyCode
        if (expectedCode == null) {
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                "application/json; charset=utf-8",
                """{"verified":false}""",
            )
        }
        val code = session.parameters["code"]?.firstOrNull()
        val verified = code == expectedCode
        return jsonResponse("""{"verified":$verified}""")
    }

    private fun readBody(session: IHTTPSession): String {
        val files = HashMap<String, String>()
        session.parseBody(files)
        return files["postData"].orEmpty()
    }

    private fun jsonResponse(body: String): Response =
        newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", body)

    private fun notFound(): Response =
        newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")

    companion object {
        const val DEFAULT_PORT = 8765
        private const val SOCKET_READ_TIMEOUT = 10_000
    }
}
