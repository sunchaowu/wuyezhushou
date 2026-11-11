package com.guanjiazhushou.assistant.data.voice.nlu

import com.guanjiazhushou.assistant.domain.voice.CandidateAction

object NluIntentFilter {
    const val PAYMENT = 0
    const val EVENT = 1
    const val FEATURE_TAG = 2
    const val NAVIGATE = 3
    const val CHITCHAT = 4
    const val UNCLEAR = 5

    fun filter(candidates: List<CandidateAction>, intentIndex: Int): List<CandidateAction> =
        when (intentIndex) {
            PAYMENT -> candidates.filter {
                it is CandidateAction.ProposePayment ||
                    it is CandidateAction.PendingVerify ||
                    it is CandidateAction.NavigateToRoom
            }
            EVENT -> candidates.filter {
                it is CandidateAction.ProposeEvent || it is CandidateAction.NavigateToRoom
            }
            FEATURE_TAG -> candidates.filter {
                it is CandidateAction.ProposeFeatureTags || it is CandidateAction.NavigateToRoom
            }
            NAVIGATE -> candidates.filter { it is CandidateAction.NavigateToRoom } +
                candidates.filter { it !is CandidateAction.NavigateToRoom }
            UNCLEAR -> candidates
            else -> emptyList()
        }
}
