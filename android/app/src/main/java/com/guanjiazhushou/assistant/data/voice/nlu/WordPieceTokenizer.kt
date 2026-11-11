package com.guanjiazhushou.assistant.data.voice.nlu

import android.content.Context
import org.json.JSONObject

data class TokenizerMeta(
    val unkToken: String,
    val clsToken: String,
    val sepToken: String,
    val padToken: String,
    val maxLength: Int,
)

data class EncodedInput(
    val inputIds: LongArray,
    val attentionMask: LongArray,
)

/**
 * Bert-style WordPiece aligned with HuggingFace BertTokenizer for Chinese:
 * CJK characters are treated as individual basic tokens, then greedily matched in vocab.
 */
class WordPieceTokenizer(
    private val vocab: Map<String, Int>,
    private val meta: TokenizerMeta,
) {
    private val unkId = vocab.getValue(meta.unkToken)
    private val clsId = vocab.getValue(meta.clsToken)
    private val sepId = vocab.getValue(meta.sepToken)
    private val padId = vocab.getValue(meta.padToken)

    fun encode(text: String): EncodedInput {
        val normalized = text.replace(Regex("\\s+"), "")
        val pieces = tokenize(normalized)
        val maxBody = (meta.maxLength - 2).coerceAtLeast(0)
        val body = pieces.take(maxBody)
        val ids = LongArray(meta.maxLength) { padId.toLong() }
        val mask = LongArray(meta.maxLength)
        ids[0] = clsId.toLong()
        mask[0] = 1L
        var i = 1
        for (pieceId in body) {
            if (i >= meta.maxLength - 1) break
            ids[i] = pieceId.toLong()
            mask[i] = 1L
            i++
        }
        if (i < meta.maxLength) {
            ids[i] = sepId.toLong()
            mask[i] = 1L
        }
        return EncodedInput(ids, mask)
    }

    private fun tokenize(text: String): List<Int> {
        val out = ArrayList<Int>()
        for (ch in text) {
            out.addAll(wordPieceToken(ch.toString()))
        }
        return out
    }

    private fun wordPieceToken(token: String): List<Int> {
        if (vocab.containsKey(token)) {
            return listOf(vocab.getValue(token))
        }
        val chars = token.toCharArray()
        if (chars.isEmpty()) return emptyList()
        val subTokens = ArrayList<Int>()
        var start = 0
        while (start < chars.size) {
            var end = chars.size
            var cur: Int? = null
            while (start < end) {
                var substr = String(chars, start, end - start)
                if (start > 0) {
                    substr = "##$substr"
                }
                val id = vocab[substr]
                if (id != null) {
                    cur = id
                    break
                }
                end--
            }
            if (cur == null) {
                return listOf(unkId)
            }
            subTokens.add(cur)
            start = end
        }
        return subTokens
    }

    companion object {
        const val VOCAB_ASSET = "nlu/vocab.txt"
        const val META_ASSET = "nlu/tokenizer_meta.json"

        fun fromAssets(context: Context): WordPieceTokenizer {
            val metaJson = context.assets.open(META_ASSET).bufferedReader().use { it.readText() }
            val obj = JSONObject(metaJson)
            val meta = TokenizerMeta(
                unkToken = obj.getString("unk_token"),
                clsToken = obj.getString("cls_token"),
                sepToken = obj.getString("sep_token"),
                padToken = obj.getString("pad_token"),
                maxLength = obj.getInt("max_length"),
            )
            val vocab = LinkedHashMap<String, Int>()
            context.assets.open(VOCAB_ASSET).bufferedReader().useLines { lines ->
                lines.forEachIndexed { index, line ->
                    vocab[line] = index
                }
            }
            require(vocab.isNotEmpty()) { "NLU vocab empty: $VOCAB_ASSET" }
            require(vocab.containsKey(meta.unkToken)) { "vocab missing ${meta.unkToken}" }
            require(vocab.containsKey(meta.clsToken)) { "vocab missing ${meta.clsToken}" }
            require(vocab.containsKey(meta.sepToken)) { "vocab missing ${meta.sepToken}" }
            require(vocab.containsKey(meta.padToken)) { "vocab missing ${meta.padToken}" }
            return WordPieceTokenizer(vocab, meta)
        }
    }
}
