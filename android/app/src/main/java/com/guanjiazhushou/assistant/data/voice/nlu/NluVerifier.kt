package com.guanjiazhushou.assistant.data.voice.nlu

import com.guanjiazhushou.assistant.domain.voice.CandidateAction

data class NluVerificationInput(
    val rawText: String,
    val candidates: List<CandidateAction>,
)

data class NluVerificationResult(
    val shouldAct: Boolean,
    val confidence: Float,
    val filteredActions: List<CandidateAction>,
    val intentLabel: String,
)

interface NluVerifier {
    fun verify(input: NluVerificationInput): NluVerificationResult
}
