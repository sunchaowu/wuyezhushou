package com.guanjiazhushou.assistant.data.audio

import android.media.MediaPlayer
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomAudioPlayer @Inject constructor() {
    private var mediaPlayer: MediaPlayer? = null
    private var playingPath: String? = null
    var onPlaybackComplete: (() -> Unit)? = null

    val currentPath: String?
        get() = playingPath

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    fun play(file: File, onComplete: () -> Unit) {
        stop()
        val player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            setOnCompletionListener {
                stop()
                onComplete()
            }
            start()
        }
        mediaPlayer = player
        playingPath = file.absolutePath
        onPlaybackComplete = onComplete
    }

    fun stop() {
        mediaPlayer?.runCatching {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        playingPath = null
        onPlaybackComplete = null
    }
}
