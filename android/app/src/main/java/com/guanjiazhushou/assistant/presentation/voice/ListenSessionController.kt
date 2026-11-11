package com.guanjiazhushou.assistant.presentation.voice

import android.os.Handler
import android.os.Looper
import com.guanjiazhushou.assistant.data.voice.asr.OfflineAsrEngine
import com.guanjiazhushou.assistant.data.voice.nlu.NluVerificationInput
import com.guanjiazhushou.assistant.data.voice.nlu.NluVerifier
import com.guanjiazhushou.assistant.data.voice.nlu.RuleSemanticParser
import com.guanjiazhushou.assistant.data.voice.nlu.TagLexiconBuilder
import com.guanjiazhushou.assistant.domain.model.FeatureTag
import com.guanjiazhushou.assistant.domain.model.RoomWithFeatureIds
import com.guanjiazhushou.assistant.domain.voice.CandidateAction
import com.guanjiazhushou.assistant.domain.voice.ListenIntent
import com.guanjiazhushou.assistant.domain.voice.ListenSessionState
import com.guanjiazhushou.assistant.domain.voice.NAVIGATE_COOLDOWN_MS
import com.guanjiazhushou.assistant.domain.voice.PENDING_VERIFY_TIMEOUT_MS
import com.guanjiazhushou.assistant.domain.voice.ParseContext
import com.guanjiazhushou.assistant.domain.voice.PendingVerification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class ListenUiEvent(
    val proposals: List<CandidateAction> = emptyList(),
    val navigateRoomId: Long? = null,
    val ambiguousRoomNumbers: List<String> = emptyList(),
    val intent: ListenIntent? = null,
)

@Singleton
class ListenSessionController @Inject constructor(
    private val parser: RuleSemanticParser,
    private val nluVerifier: NluVerifier,
    private val asrEngine: OfflineAsrEngine,
) {
    private val _state = MutableStateFlow(ListenSessionState())
    val state: StateFlow<ListenSessionState> = _state.asStateFlow()

    private val _uiEvent = MutableStateFlow(ListenUiEvent())
    val uiEvent: StateFlow<ListenUiEvent> = _uiEvent.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())

    fun enterBuilding(
        buildingId: Long,
        rooms: List<RoomWithFeatureIds>,
        featureTags: List<FeatureTag>,
    ) {
        val numberToId = rooms.associate { it.room.roomNumber to it.room.id }
        _state.value = ListenSessionState(
            buildingId = buildingId,
            isListening = true,
            isMuted = false,
            knownRoomNumbers = numberToId.keys,
            roomNumberToId = numberToId,
            activeTagLexicon = TagLexiconBuilder.build(featureTags),
        )
        _uiEvent.value = ListenUiEvent()
        tryStartAsr()
    }

    fun leaveBuilding() {
        runCatching { asrEngine.stop() }
        _state.value = ListenSessionState()
        _uiEvent.value = ListenUiEvent()
    }

    private fun tryStartAsr() {
        try {
            asrEngine.start(
                onPartial = { text ->
                    mainHandler.post {
                        _state.update { it.copy(liveTranscript = text, lastError = null) }
                    }
                },
                onFinal = { text ->
                    mainHandler.post {
                        _state.update { it.copy(liveTranscript = text) }
                        processTranscript(text)
                    }
                },
            )
            _state.update { it.copy(lastError = null, liveTranscript = "") }
        } catch (error: Exception) {
            // Keep text-sim listen path; surface ASR readiness error without silent fallback.
            _state.update {
                it.copy(lastError = error.message ?: "离线 ASR 未启动")
            }
        }
    }

    fun setMuted(muted: Boolean) {
        _state.update { it.copy(isMuted = muted) }
        if (muted) {
            runCatching { asrEngine.stop() }
        } else if (_state.value.isListening) {
            tryStartAsr()
        }
    }

    /** After mic permission grant, restart ASR without leaving the building session. */
    fun retryAsr() {
        if (!_state.value.isListening || _state.value.isMuted) return
        runCatching { asrEngine.stop() }
        tryStartAsr()
    }

    fun updateFocus(roomId: Long?, roomNumber: String?) {
        _state.update { it.copy(focusedRoomId = roomId, focusedRoomNumber = roomNumber) }
    }

    fun refreshRooms(rooms: List<RoomWithFeatureIds>, featureTags: List<FeatureTag>) {
        val numberToId = rooms.associate { it.room.roomNumber to it.room.id }
        _state.update {
            it.copy(
                knownRoomNumbers = numberToId.keys,
                roomNumberToId = numberToId,
                activeTagLexicon = TagLexiconBuilder.build(featureTags),
            )
        }
    }

    fun clearUiEvent() {
        _uiEvent.value = ListenUiEvent()
    }

    fun consumeNavigation() {
        _uiEvent.update { it.copy(navigateRoomId = null) }
    }

    fun consumeAmbiguousRooms() {
        _uiEvent.update { it.copy(ambiguousRoomNumbers = emptyList()) }
    }

    fun consumeProposals() {
        _uiEvent.update { it.copy(proposals = emptyList()) }
    }

    fun clearError() {
        _state.update { it.copy(lastError = null) }
    }

    /** M3 text-sim ASR entry and M5 final transcript sink. */
    fun processTranscript(rawText: String) {
        val snapshot = _state.value
        if (!snapshot.isListening || snapshot.isMuted || snapshot.buildingId == 0L) return

        val now = System.currentTimeMillis()
        var pending = snapshot.pendingVerification
        if (pending != null && now - pending.createdAtMs > PENDING_VERIFY_TIMEOUT_MS) {
            pending = null
            _state.update { it.copy(pendingVerification = null) }
        }

        val parseResult = parser.parse(
            rawText,
            ParseContext(
                buildingId = snapshot.buildingId,
                focusedRoomNumber = snapshot.focusedRoomNumber,
                knownRoomNumbers = snapshot.knownRoomNumbers,
                tagLexicon = snapshot.activeTagLexicon,
                pendingVerification = pending,
            ),
        )

        val nonIgnore = parseResult.actions.filter { it !is CandidateAction.Ignore }
        if (nonIgnore.isEmpty()) return

        val verified = try {
            nluVerifier.verify(
                NluVerificationInput(
                    rawText = rawText,
                    candidates = nonIgnore,
                ),
            )
        } catch (error: Exception) {
            _state.update { it.copy(lastError = error.message ?: "NLU 复核失败") }
            return
        }

        if (!verified.shouldAct) return

        val actions = verified.filteredActions
        val nextPending = actions.filterIsInstance<CandidateAction.PendingVerify>().firstOrNull()?.let {
            PendingVerification(
                roomNumber = it.roomNumber,
                topic = it.topic,
                createdAtMs = now,
            )
        }
        if (nextPending != null) {
            _state.update { it.copy(pendingVerification = nextPending) }
        } else if (actions.any { it is CandidateAction.ProposePayment && it.speechAct.name.startsWith("ANSWER") }) {
            _state.update { it.copy(pendingVerification = null) }
        }

        val navigate = actions.filterIsInstance<CandidateAction.NavigateToRoom>().firstOrNull()
        var navigateRoomId: Long? = null
        var ambiguous = emptyList<String>()
        if (navigate != null) {
            val matches = snapshot.roomNumberToId.filterKeys { it == navigate.roomNumber }
            when (matches.size) {
                0 -> Unit
                1 -> {
                    val roomId = matches.values.first()
                    val cooled = snapshot.lastNavigatedRoomId == roomId &&
                        now - snapshot.lastNavigateAtMs < NAVIGATE_COOLDOWN_MS
                    if (!cooled) {
                        navigateRoomId = roomId
                        _state.update {
                            it.copy(
                                lastNavigatedRoomId = roomId,
                                lastNavigateAtMs = now,
                                focusedRoomId = roomId,
                                focusedRoomNumber = navigate.roomNumber,
                            )
                        }
                    }
                }
                else -> ambiguous = matches.keys.toList()
            }
        }

        val proposals = actions.filter {
            it is CandidateAction.ProposePayment ||
                it is CandidateAction.ProposeEvent ||
                it is CandidateAction.ProposeFeatureTags
        }
        val listenIntent = ListenIntent.fromLabel(
            label = verified.intentLabel,
            confidence = verified.confidence,
            shouldAct = verified.shouldAct,
        )
        if (proposals.isNotEmpty() || navigateRoomId != null || ambiguous.isNotEmpty()) {
            _uiEvent.value = ListenUiEvent(
                proposals = proposals,
                navigateRoomId = navigateRoomId,
                ambiguousRoomNumbers = ambiguous,
                intent = listenIntent.takeIf { proposals.isNotEmpty() && it.shouldAct },
            )
        }
    }
}
