package com.guanjiazhushou.assistant.data.voice.nlu

import com.guanjiazhushou.assistant.domain.model.FeatureTag
import com.guanjiazhushou.assistant.domain.model.IconType
import com.guanjiazhushou.assistant.domain.model.PaymentType
import com.guanjiazhushou.assistant.domain.voice.CandidateAction
import com.guanjiazhushou.assistant.domain.voice.ParseContext
import com.guanjiazhushou.assistant.domain.voice.PendingVerification
import com.guanjiazhushou.assistant.domain.voice.SpeechAct
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleSemanticParserTest {
    private val parser = RuleSemanticParser()

    private val rooms = setOf("201", "601", "602", "603")

    private val tags = listOf(
        FeatureTag(
            id = 1,
            name = "宠物",
            iconType = IconType.BUILTIN,
            iconKey = "pet",
            isBuiltin = true,
            createdAt = 0L,
        ),
        FeatureTag(
            id = 2,
            name = "保姆住",
            iconType = IconType.EMOJI,
            iconKey = "护",
            isBuiltin = false,
            createdAt = 0L,
            description = "家里请了住家保姆或护工长期居住",
        ),
        FeatureTag(
            id = 3,
            name = "空关难说",
            iconType = IconType.EMOJI,
            iconKey = "空",
            isBuiltin = false,
            createdAt = 0L,
            description = null,
        ),
    )

    private fun context(
        focused: String? = null,
        pending: PendingVerification? = null,
    ): ParseContext = ParseContext(
        buildingId = 1L,
        focusedRoomNumber = focused,
        knownRoomNumbers = rooms,
        tagLexicon = TagLexiconBuilder.build(tags),
        pendingVerification = pending,
    )

    @Test
    fun arrearsThreeMonthsProducesPaymentProposalAndNavigate() {
        val result = parser.parse("601欠费三个月", context())
        assertTrue(result.actions.any { it is CandidateAction.NavigateToRoom && it.roomNumber == "601" })
        val payment = result.actions.filterIsInstance<CandidateAction.ProposePayment>().single()
        assertEquals("601", payment.roomNumber)
        assertEquals(PaymentType.ARREARS, payment.paymentType)
        assertEquals(3, payment.months)
    }

    @Test
    fun leakProducesEventAndNavigate() {
        val result = parser.parse("602刚刚说家里漏水，需要工程上面维修", context())
        assertTrue(result.actions.any { it is CandidateAction.NavigateToRoom && it.roomNumber == "602" })
        val event = result.actions.filterIsInstance<CandidateAction.ProposeEvent>().single()
        assertEquals("repair", event.eventType)
        assertEquals("602", event.roomNumber)
    }

    @Test
    fun noisyProducesNavigateWithBusinessWord() {
        val result = parser.parse("一单元201现在楼上很吵", context())
        val nav = result.actions.filterIsInstance<CandidateAction.NavigateToRoom>().single()
        assertEquals("201", nav.roomNumber)
        assertTrue(result.actions.any { it is CandidateAction.ProposeEvent })
    }

    @Test
    fun roomOnlyDoesNotNavigate() {
        val result = parser.parse("602", context())
        assertFalse(result.actions.any { it is CandidateAction.NavigateToRoom })
        assertTrue(result.actions.any { it is CandidateAction.Ignore })
    }

    @Test
    fun petTagMatched() {
        val result = parser.parse("602有宠物", context())
        val tagsAction = result.actions.filterIsInstance<CandidateAction.ProposeFeatureTags>().single()
        assertEquals(listOf(1L), tagsAction.tagIds)
        assertEquals("602", tagsAction.roomNumber)
    }

    @Test
    fun customTagMatchedViaDescription() {
        val result = parser.parse("请了个保姆住这儿", context(focused = "603"))
        val tagsAction = result.actions.filterIsInstance<CandidateAction.ProposeFeatureTags>().single()
        assertTrue(tagsAction.tagIds.contains(2L))
        assertEquals("603", tagsAction.roomNumber)
    }

    @Test
    fun customTagWithoutDescriptionNotHardcoded() {
        val result = parser.parse("这户长期没人住感觉空关", context(focused = "603"))
        val tagActions = result.actions.filterIsInstance<CandidateAction.ProposeFeatureTags>()
        assertFalse(tagActions.any { 3L in it.tagIds })
    }

    @Test
    fun questionThenNoAnswerUpdatesArrears() {
        val pending = PendingVerification(roomNumber = "602", topic = "payment", createdAtMs = 1L)
        val result = parser.parse("没收到", context(pending = pending))
        val payment = result.actions.filterIsInstance<CandidateAction.ProposePayment>().single()
        assertEquals(SpeechAct.ANSWER_NO, payment.speechAct)
        assertEquals(PaymentType.ARREARS, payment.paymentType)
        assertEquals("602", payment.roomNumber)
    }

    @Test
    fun amountNeedsClarification() {
        val result = parser.parse("602刚收到100元", context())
        val payment = result.actions.filterIsInstance<CandidateAction.ProposePayment>().single()
        assertTrue(payment.needsAmountClarification)
        assertEquals(100, payment.amountYuan)
    }

    @Test
    fun chitchatIgnored() {
        val result = parser.parse("中午吃啥", context())
        assertEquals(listOf(CandidateAction.Ignore), result.actions)
    }
}
