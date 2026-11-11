package com.guanjiazhushou.assistant.domain.voice

import com.guanjiazhushou.assistant.domain.model.PaymentType

sealed class CandidateAction {
    data object Ignore : CandidateAction()

    data class NavigateToRoom(
        val roomNumber: String,
        val unitHint: Int? = null,
        val reasonBusinessWord: String,
    ) : CandidateAction()

    data class ProposePayment(
        val roomNumber: String?,
        val paymentType: PaymentType?,
        val months: Int?,
        val amountYuan: Int?,
        val speechAct: SpeechAct,
        val rawText: String,
        val needsAmountClarification: Boolean = false,
    ) : CandidateAction()

    data class ProposeEvent(
        val roomNumber: String?,
        val eventType: String,
        val summary: String,
        val rawText: String,
    ) : CandidateAction()

    data class ProposeFeatureTags(
        val roomNumber: String?,
        val tagIds: List<Long>,
        val remove: Boolean = false,
        val rawText: String,
    ) : CandidateAction()

    data class PendingVerify(
        val roomNumber: String?,
        val topic: String,
        val rawText: String,
    ) : CandidateAction()
}

enum class SpeechAct {
    ASSERT,
    QUESTION,
    HEARSAY,
    ANSWER_YES,
    ANSWER_NO,
}

data class ParseContext(
    val buildingId: Long,
    val focusedRoomNumber: String? = null,
    val knownRoomNumbers: Set<String> = emptySet(),
    val tagLexicon: List<TagLexiconEntry> = emptyList(),
    val pendingVerification: PendingVerification? = null,
)

data class PendingVerification(
    val roomNumber: String?,
    val topic: String,
    val createdAtMs: Long,
)

data class TagLexiconEntry(
    val tagId: Long,
    val name: String,
    val matchTerms: List<String>,
)

data class ParseResult(
    val actions: List<CandidateAction>,
)
