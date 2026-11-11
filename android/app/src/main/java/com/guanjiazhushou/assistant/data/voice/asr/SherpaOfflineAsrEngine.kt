package com.guanjiazhushou.assistant.data.voice.asr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import com.k2fsa.sherpa.onnx.EndpointConfig
import com.k2fsa.sherpa.onnx.EndpointRule
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Offline streaming ASR via sherpa-onnx Zipformer-zh under assets/asr/.
 * Missing assets/native/mic permission fails loudly — no cloud or fake ASR fallback.
 */
class SherpaOfflineAsrEngine(
    private val context: Context,
) : OfflineAsrEngine {
    private val started = AtomicBoolean(false)
    private val recording = AtomicBoolean(false)

    @Volatile
    private var recognizer: OnlineRecognizer? = null

    @Volatile
    private var audioRecord: AudioRecord? = null

    @Volatile
    private var recordThread: Thread? = null

    override val isReady: Boolean
        get() = hasRequiredAssets(context.assets)

    override fun start(onPartial: (String) -> Unit, onFinal: (String) -> Unit) {
        if (!isReady) {
            error(
                "离线 ASR 未就绪：请将 Zipformer-zh 模型放入 assets/asr/ " +
                    "(需包含 $MODEL_DIR 下的 encoder/decoder/joiner/tokens)，" +
                    "并集成 sherpa-onnx Android AAR。当前不可用，未降级到云端或假识别。",
            )
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            error("未授予麦克风权限，无法启动离线听记 ASR（未降级）。")
        }
        if (!tryLoadNative()) {
            error(
                "sherpa-onnx native 库未加载。请将 sherpa-onnx Android AAR 加入依赖后重试。" +
                    "未降级使用。",
            )
        }
        if (!started.compareAndSet(false, true)) {
            return
        }
        try {
            val online = createRecognizer()
            recognizer = online
            if (!initMicrophone()) {
                releaseInternal()
                error("无法初始化 AudioRecord，离线 ASR 启动失败（未降级）。")
            }
            recording.set(true)
            audioRecord!!.startRecording()
            recordThread = thread(name = "sherpa-asr", start = true) {
                processSamples(online, onPartial, onFinal)
            }
        } catch (error: Throwable) {
            releaseInternal()
            throw error
        }
    }

    override fun stop() {
        recording.set(false)
        started.set(false)
        runCatching { audioRecord?.stop() }
        recordThread?.join(1_500)
        recordThread = null
        runCatching { audioRecord?.release() }
        audioRecord = null
        runCatching { recognizer?.release() }
        recognizer = null
    }

    override fun release() {
        stop()
    }

    private fun releaseInternal() {
        recording.set(false)
        started.set(false)
        runCatching { audioRecord?.stop() }
        runCatching { audioRecord?.release() }
        audioRecord = null
        recordThread = null
        runCatching { recognizer?.release() }
        recognizer = null
    }

    private fun createRecognizer(): OnlineRecognizer {
        val modelConfig = OnlineModelConfig(
            transducer = OnlineTransducerModelConfig(
                encoder = "$MODEL_DIR/encoder.onnx",
                decoder = "$MODEL_DIR/decoder.onnx",
                joiner = "$MODEL_DIR/joiner.onnx",
            ),
            tokens = "$MODEL_DIR/tokens.txt",
            numThreads = 2,
            provider = "cpu",
            debug = false,
            // Official catalog type=9 for zh-14M-2023-02-23
            modelType = "zipformer",
        )
        val config = OnlineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80),
            modelConfig = modelConfig,
            endpointConfig = EndpointConfig(
                rule1 = EndpointRule(false, 2.4f, 0.0f),
                rule2 = EndpointRule(true, 1.4f, 0.0f),
                rule3 = EndpointRule(false, 0.0f, 20.0f),
            ),
            enableEndpoint = true,
            decodingMethod = "greedy_search",
            maxActivePaths = 4,
        )
        return OnlineRecognizer(
            assetManager = context.assets,
            config = config,
        )
    }

    private fun initMicrophone(): Boolean {
        val minBytes = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBytes == AudioRecord.ERROR || minBytes == AudioRecord.ERROR_BAD_VALUE) {
            return false
        }
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBytes * 2,
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return false
        }
        audioRecord = record
        return true
    }

    private fun processSamples(
        online: OnlineRecognizer,
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
    ) {
        var stream: OnlineStream? = null
        try {
            stream = online.createStream()
            val bufferSize = (0.1 * SAMPLE_RATE).toInt()
            val buffer = ShortArray(bufferSize)
            var lastPartial = ""
            while (recording.get()) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: break
                if (read <= 0) continue
                val samples = FloatArray(read) { buffer[it] / 32768.0f }
                stream.acceptWaveform(samples, sampleRate = SAMPLE_RATE)
                while (online.isReady(stream)) {
                    online.decode(stream)
                }
                val text = online.getResult(stream).text.trim()
                val endpoint = online.isEndpoint(stream)
                if (text.isNotEmpty() && text != lastPartial) {
                    lastPartial = text
                    runCatching { onPartial(text) }
                }
                if (endpoint) {
                    if (text.isNotEmpty()) {
                        runCatching { onFinal(text) }
                    }
                    online.reset(stream)
                    lastPartial = ""
                }
            }
        } catch (error: Throwable) {
            Log.e(TAG, "ASR loop failed", error)
        } finally {
            runCatching { stream?.release() }
        }
    }

    private fun tryLoadNative(): Boolean =
        try {
            System.loadLibrary("sherpa-onnx-jni")
            true
        } catch (_: UnsatisfiedLinkError) {
            false
        }

    companion object {
        private const val TAG = "SherpaAsr"
        const val MODEL_DIR = "asr"
        const val SAMPLE_RATE = 16_000
        val REQUIRED_ASSET_FILES = listOf(
            "tokens.txt",
            "encoder.onnx",
            "decoder.onnx",
            "joiner.onnx",
        )

        fun hasRequiredAssets(assets: AssetManager): Boolean =
            try {
                val names = assets.list(MODEL_DIR)?.toSet().orEmpty()
                REQUIRED_ASSET_FILES.all { file ->
                    names.contains(file) || names.any { it.endsWith(file) }
                }
            } catch (_: Exception) {
                false
            }
    }
}
