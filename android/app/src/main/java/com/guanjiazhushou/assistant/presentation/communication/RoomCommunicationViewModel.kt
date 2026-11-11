package com.guanjiazhushou.assistant.presentation.communication

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guanjiazhushou.assistant.data.audio.RoomAudioPlayer
import com.guanjiazhushou.assistant.data.audio.RoomAudioRecorder
import com.guanjiazhushou.assistant.domain.model.RoomCommunicationRecord
import com.guanjiazhushou.assistant.domain.repository.BuildingRepository
import com.guanjiazhushou.assistant.domain.repository.RoomCommunicationRepository
import com.guanjiazhushou.assistant.domain.repository.RoomRepository
import com.guanjiazhushou.assistant.presentation.navigation.getNavLong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoomCommunicationUiState(
    val roomNumber: String = "",
    val buildingName: String = "",
    val isReadonly: Boolean = false,
    val records: List<RoomCommunicationRecord> = emptyList(),
    val isRecording: Boolean = false,
    val recordingElapsedMs: Long = 0,
    val playingRecordId: Long? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class RoomCommunicationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val roomRepository: RoomRepository,
    private val buildingRepository: BuildingRepository,
    private val communicationRepository: RoomCommunicationRepository,
    private val audioRecorder: RoomAudioRecorder,
    private val audioPlayer: RoomAudioPlayer,
) : ViewModel() {

    private val roomId: Long = checkNotNull(savedStateHandle.getNavLong("roomId"))

    private val _roomMeta = MutableStateFlow(RoomMeta())
    private val _isRecording = MutableStateFlow(false)
    private val _recordingElapsedMs = MutableStateFlow(0L)
    private val _playingRecordId = MutableStateFlow<Long?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private var recordingTimerJob: Job? = null

    val uiState: StateFlow<RoomCommunicationUiState> = combine(
        combine(
            _roomMeta,
            communicationRepository.observeByRoom(roomId),
        ) { meta, records ->
            meta to records
        },
        combine(_isRecording, _recordingElapsedMs, _playingRecordId, _errorMessage) {
                isRecording, elapsed, playingId, error ->
            RecordingBundle(isRecording, elapsed, playingId, error)
        },
    ) { (meta, records), recording ->
        RoomCommunicationUiState(
            roomNumber = meta.roomNumber,
            buildingName = meta.buildingName,
            isReadonly = meta.isReadonly,
            records = records,
            isRecording = recording.isRecording,
            recordingElapsedMs = recording.elapsedMs,
            playingRecordId = recording.playingId,
            errorMessage = recording.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RoomCommunicationUiState(),
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val room = roomRepository.getById(roomId) ?: return@launch
            val building = buildingRepository.getById(room.buildingId)
            _roomMeta.value = RoomMeta(
                roomNumber = room.roomNumber,
                buildingName = building?.name.orEmpty(),
                isReadonly = building?.isReadonly == true,
            )
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun sendTextMessage(content: String) {
        if (_roomMeta.value.isReadonly) {
            _errorMessage.value = "只读楼栋不可添加沟通记录"
            return
        }
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                communicationRepository.addTextRecord(roomId, trimmed)
            }.onFailure { error ->
                _errorMessage.update { error.message ?: "保存文字记录失败" }
            }
        }
    }

    fun toggleRecording() {
        if (_roomMeta.value.isReadonly) {
            _errorMessage.value = "只读楼栋不可录音"
            return
        }
        if (_isRecording.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    fun cancelRecording() {
        if (!_isRecording.value) return
        audioRecorder.cancel()
        stopRecordingTimer()
        _isRecording.value = false
        _recordingElapsedMs.value = 0L
    }

    fun togglePlayback(record: RoomCommunicationRecord) {
        val audioPath = record.audioPath ?: return
        if (_playingRecordId.value == record.id && audioPlayer.isPlaying) {
            audioPlayer.stop()
            _playingRecordId.value = null
            return
        }
        val file = communicationRepository.resolveAudioFile(audioPath)
        if (!file.exists()) {
            _errorMessage.value = "录音文件不存在"
            return
        }
        audioPlayer.stop()
        _playingRecordId.value = record.id
        audioPlayer.play(file) {
            _playingRecordId.value = null
        }
    }

    private fun startRecording() {
        runCatching {
            audioRecorder.start(roomId)
        }.onSuccess {
            _isRecording.value = true
            _recordingElapsedMs.value = 0L
            startRecordingTimer()
        }.onFailure { error ->
            _errorMessage.value = error.message ?: "无法开始录音"
        }
    }

    private fun stopRecording() {
        val result = audioRecorder.stop()
        stopRecordingTimer()
        _isRecording.value = false
        _recordingElapsedMs.value = 0L
        if (result == null) return
        if (result.durationMs < 500) {
            communicationRepository.resolveAudioFile(result.relativePath).delete()
            _errorMessage.value = "录音时间过短，请重新录制"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                communicationRepository.addAudioRecord(
                    roomId = roomId,
                    audioPath = result.relativePath,
                    durationMs = result.durationMs,
                )
            }.onFailure { error ->
                _errorMessage.update { error.message ?: "保存录音失败" }
            }
        }
    }

    private fun startRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            while (_isRecording.value) {
                delay(200)
                _recordingElapsedMs.value = _recordingElapsedMs.value + 200
            }
        }
    }

    private fun stopRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
    }

    override fun onCleared() {
        audioRecorder.cancel()
        audioPlayer.stop()
        super.onCleared()
    }

    private data class RoomMeta(
        val roomNumber: String = "",
        val buildingName: String = "",
        val isReadonly: Boolean = false,
    )

    private data class RecordingBundle(
        val isRecording: Boolean,
        val elapsedMs: Long,
        val playingId: Long?,
        val error: String?,
    )
}
