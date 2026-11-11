package com.guanjiazhushou.assistant.data.voice.nlu

import com.guanjiazhushou.assistant.domain.voice.CandidateAction
import com.guanjiazhushou.assistant.domain.voice.SpeechAct
import org.junit.Assert.assertTrue
import org.junit.Test

class OnDeviceNluFilterTest {
    @Test
    fun filterByIntent_paymentKeepsPaymentOnly() {
        val candidates = listOf(
            CandidateAction.ProposePayment(
                roomNumber = "601",
                months = 3,
                amountYuan = null,
                paymentType = null,
                speechAct = SpeechAct.ASSERT,
                rawText = "601欠费三个月",
                needsAmountClarification = false,
            ),
            CandidateAction.ProposeEvent(
                roomNumber = "601",
                eventType = "noise",
                summary = "吵",
                rawText = "x",
            ),
        )
        val filtered = NluIntentFilter.filter(candidates, intentIndex = 0) // payment
        assertTrue(filtered.all { it is CandidateAction.ProposePayment || it is CandidateAction.NavigateToRoom || it is CandidateAction.PendingVerify })
        assertTrue(filtered.any { it is CandidateAction.ProposePayment })
        assertTrue(filtered.none { it is CandidateAction.ProposeEvent })
    }

    @Test
    fun emptyFilterMeansNoFallback() {
        val filtered = NluIntentFilter.filter(emptyList(), intentIndex = 0)
        assertTrue(filtered.isEmpty())
    }
}
