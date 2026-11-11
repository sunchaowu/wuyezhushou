package com.guanjiazhushou.assistant.data.voice.nlu

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import java.nio.LongBuffer
import kotlin.math.exp

/**
 * On-device NLU verifier backed by text-encoder ONNX (WordPiece → intent + should_act).
 * Missing model / vocab / tokenizer meta is a hard error (no heuristic degrade).
 */
class OnDeviceNluVerifier(
    context: Context,
) : NluVerifier {
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val tokenizer: WordPieceTokenizer

    init {
        requireAsset(context, MODEL_ASSET)
        requireAsset(context, WordPieceTokenizer.VOCAB_ASSET)
        requireAsset(context, WordPieceTokenizer.META_ASSET)
        val bytes = context.assets.open(MODEL_ASSET).use { it.readBytes() }
        require(bytes.isNotEmpty()) { "NLU 模型为空: $MODEL_ASSET" }
        require(bytes.size > MIN_MODEL_BYTES) {
            "NLU 模型过小(疑似线性 stub): $MODEL_ASSET size=${bytes.size}"
        }
        session = env.createSession(bytes)
        tokenizer = WordPieceTokenizer.fromAssets(context)
    }

    override fun verify(input: NluVerificationInput): NluVerificationResult {
        val encoded = tokenizer.encode(input.rawText)
        val intentLogits: FloatArray
        val shouldActLogit: Float
        OnnxTensor.createTensor(
            env,
            LongBuffer.wrap(encoded.inputIds),
            longArrayOf(1, encoded.inputIds.size.toLong()),
        ).use { idsTensor ->
            OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(encoded.attentionMask),
                longArrayOf(1, encoded.attentionMask.size.toLong()),
            ).use { maskTensor ->
                session.run(
                    mapOf(
                        "input_ids" to idsTensor,
                        "attention_mask" to maskTensor,
                    ),
                ).use { result ->
                    intentLogits = (result.get(0).value as Array<FloatArray>)[0]
                    val actRaw = result.get(1).value
                    shouldActLogit = when (actRaw) {
                        is Array<*> -> (actRaw[0] as FloatArray)[0]
                        is FloatArray -> actRaw[0]
                        else -> error("Unexpected should_act output: ${actRaw?.javaClass}")
                    }
                }
            }
        }

        val intentIndex = intentLogits.indices.maxByOrNull { intentLogits[it] } ?: NluIntentFilter.CHITCHAT
        val intentLabel = INTENT_LABELS.getOrElse(intentIndex) { "unclear" }
        val shouldActProb = sigmoid(shouldActLogit)
        val shouldAct = shouldActProb >= SHOULD_ACT_THRESHOLD && intentIndex != NluIntentFilter.CHITCHAT
        val filtered = NluIntentFilter.filter(input.candidates, intentIndex)
        val shouldActFinal = shouldAct && filtered.isNotEmpty()
        return NluVerificationResult(
            shouldAct = shouldActFinal,
            confidence = softmaxMax(intentLogits),
            filteredActions = if (shouldActFinal) filtered else emptyList(),
            intentLabel = intentLabel,
        )
    }

    private fun requireAsset(context: Context, name: String) {
        val exists = runCatching {
            context.assets.open(name).use { it.read() >= 0 }
        }.getOrDefault(false)
        require(exists) { "NLU 资产缺失: $name" }
    }

    private fun sigmoid(x: Float): Float = (1.0 / (1.0 + exp(-x.toDouble()))).toFloat()

    private fun softmaxMax(logits: FloatArray): Float {
        val max = logits.maxOrNull() ?: return 0f
        var sum = 0.0
        val exps = DoubleArray(logits.size)
        for (i in logits.indices) {
            exps[i] = exp((logits[i] - max).toDouble())
            sum += exps[i]
        }
        if (sum <= 0.0) return 0f
        return (exps.maxOrNull()!! / sum).toFloat()
    }

    companion object {
        const val MODEL_ASSET = "nlu/listen_intent.onnx"
        private const val MIN_MODEL_BYTES = 50_000
        private const val SHOULD_ACT_THRESHOLD = 0.45f
        private val INTENT_LABELS = listOf(
            "payment",
            "event",
            "feature_tag",
            "navigate_context",
            "chitchat",
            "unclear",
        )
    }
}
