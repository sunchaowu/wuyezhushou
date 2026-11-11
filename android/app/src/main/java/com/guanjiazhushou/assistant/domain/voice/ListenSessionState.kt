package com.guanjiazhushou.assistant.domain.voice

data class ListenSessionState(
    val buildingId: Long = 0,
    val focusedRoomId: Long? = null,
    val focusedRoomNumber: String? = null,
    val isListening: Boolean = false,
    val isMuted: Boolean = false,
    val pendingVerification: PendingVerification? = null,
    val lastNavigatedRoomId: Long? = null,
    val lastNavigateAtMs: Long = 0L,
    val activeTagLexicon: List<TagLexiconEntry> = emptyList(),
    val knownRoomNumbers: Set<String> = emptySet(),
    val roomNumberToId: Map<String, Long> = emptyMap(),
    /** Live ASR partial / last final transcript for status bar verification. */
    val liveTranscript: String = "",
    val lastError: String? = null,
)

const val NAVIGATE_COOLDOWN_MS = 10_000L
const val PENDING_VERIFY_TIMEOUT_MS = 25_000L
