package com.guanjiazhushou.assistant.data.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class AudioRecordingResult(
    val relativePath: String,
    val durationMs: Long,
)

@Singleton
class RoomAudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var mediaRecorder: MediaRecorder? = null
    private var startedAtMs: Long = 0L
    private var relativePath: String? = null

    val isRecording: Boolean
        get() = mediaRecorder != null

    fun start(roomId: Long): String {
        stop()
        val dir = File(context.filesDir, "room_communications/$roomId").apply { mkdirs() }
        val fileName = "${System.currentTimeMillis()}.m4a"
        val outputFile = File(dir, fileName)
        val path = "room_communications/$roomId/$fileName"

        val recorder = createMediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44_100)
            setAudioEncodingBitRate(96_000)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }

        mediaRecorder = recorder
        startedAtMs = System.currentTimeMillis()
        relativePath = path
        return path
    }

    fun stop(): AudioRecordingResult? {
        val recorder = mediaRecorder ?: return null
        val path = relativePath ?: return null
        val durationMs = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(0)

        runCatching {
            recorder.stop()
        }
        recorder.release()
        mediaRecorder = null
        relativePath = null
        startedAtMs = 0L

        return AudioRecordingResult(relativePath = path, durationMs = durationMs)
    }

    fun cancel() {
        val recorder = mediaRecorder ?: return
        val path = relativePath
        runCatching { recorder.stop() }
        recorder.release()
        mediaRecorder = null
        relativePath = null
        startedAtMs = 0L
        path?.let { File(context.filesDir, it).delete() }
    }

    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
}
