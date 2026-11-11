package com.guanjiazhushou.assistant.data.lan

data class LanGroupInfo(
    val groupName: String,
    val hostDeviceName: String,
    val verifyCode: String,
)

data class DiscoveredLanGroup(
    val peer: DiscoveredLanPeer,
    val groupInfo: LanGroupInfo,
)

data class LanShareOffer(
    val peer: DiscoveredLanPeer,
    val manifestJson: String,
)
