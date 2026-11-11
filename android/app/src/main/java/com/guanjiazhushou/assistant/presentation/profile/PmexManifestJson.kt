package com.guanjiazhushou.assistant.presentation.profile

import com.guanjiazhushou.assistant.domain.model.PmexManifest
import org.json.JSONArray
import org.json.JSONObject

fun PmexManifest.toManifestJson(): String {
    val buildingsJson = JSONArray()
    buildings.forEach { summary ->
        buildingsJson.put(
            JSONObject()
                .put("name", summary.name)
                .put("room_count", summary.roomCount),
        )
    }
    return JSONObject()
        .put("format_version", formatVersion)
        .put("package_type", packageType)
        .put("app_version", appVersion)
        .put("exported_at", exportedAt)
        .put("sender_device_name", senderDeviceName)
        .put("group_name", groupName)
        .put("verify_code", verifyCode)
        .put("buildings", buildingsJson)
        .put("includes_custom_icons", includesCustomIcons)
        .put("checksum_sha256", checksumSha256)
        .toString()
}
