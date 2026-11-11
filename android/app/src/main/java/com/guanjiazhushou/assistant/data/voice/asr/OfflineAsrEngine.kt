package com.guanjiazhushou.assistant.data.voice.asr

interface OfflineAsrEngine {
    val isReady: Boolean

    /** Start continuous recognition. Throws if model/assets missing. */
    fun start(onPartial: (String) -> Unit, onFinal: (String) -> Unit)

    fun stop()

    fun release()
}
