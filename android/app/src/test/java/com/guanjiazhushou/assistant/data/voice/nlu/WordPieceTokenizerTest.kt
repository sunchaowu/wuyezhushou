package com.guanjiazhushou.assistant.data.voice.nlu

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class WordPieceTokenizerTest {
    @Test
    fun encode_padsToMaxLengthWithClsSep() {
        val vocab = mapOf(
            "[PAD]" to 0,
            "[UNK]" to 1,
            "[CLS]" to 2,
            "[SEP]" to 3,
            "欠" to 10,
            "费" to 11,
            "##三" to 12,
            "个" to 13,
            "月" to 14,
        )
        val tok = WordPieceTokenizer(
            vocab = vocab,
            meta = TokenizerMeta(
                unkToken = "[UNK]",
                clsToken = "[CLS]",
                sepToken = "[SEP]",
                padToken = "[PAD]",
                maxLength = 8,
            ),
        )
        val encoded = tok.encode("欠费")
        assertEquals(8, encoded.inputIds.size)
        assertEquals(2L, encoded.inputIds[0]) // CLS
        assertEquals(10L, encoded.inputIds[1])
        assertEquals(11L, encoded.inputIds[2])
        assertEquals(3L, encoded.inputIds[3]) // SEP after 欠 费
        assertEquals(0L, encoded.inputIds[7]) // PAD
        assertArrayEquals(
            longArrayOf(1, 1, 1, 1, 0, 0, 0, 0),
            encoded.attentionMask,
        )
    }

    @Test
    fun encode_unknownCharUsesUnk() {
        val vocab = mapOf(
            "[PAD]" to 0,
            "[UNK]" to 1,
            "[CLS]" to 2,
            "[SEP]" to 3,
            "欠" to 10,
        )
        val tok = WordPieceTokenizer(
            vocab = vocab,
            meta = TokenizerMeta(
                unkToken = "[UNK]",
                clsToken = "[CLS]",
                sepToken = "[SEP]",
                padToken = "[PAD]",
                maxLength = 6,
            ),
        )
        val encoded = tok.encode("欠Z")
        assertEquals(2L, encoded.inputIds[0])
        assertEquals(10L, encoded.inputIds[1])
        assertEquals(1L, encoded.inputIds[2])
        assertEquals(3L, encoded.inputIds[3])
    }
}
