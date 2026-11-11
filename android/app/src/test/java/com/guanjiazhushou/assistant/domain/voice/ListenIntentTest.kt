package com.guanjiazhushou.assistant.domain.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class ListenIntentTest {
    @Test
    fun fromLabel_mapsPaymentDisplayName() {
        val intent = ListenIntent.fromLabel("payment", confidence = 0.82f, shouldAct = true)
        assertEquals("payment", intent.label)
        assertEquals("收费跟进", intent.displayName)
        assertEquals(0.82f, intent.confidence, 0.001f)
        assertEquals(true, intent.shouldAct)
    }

    @Test
    fun fromLabel_unknownFallsBackToUnclear() {
        val intent = ListenIntent.fromLabel("nope", confidence = 0.1f, shouldAct = false)
        assertEquals("unclear", intent.label)
        assertEquals("不明确", intent.displayName)
    }
}
