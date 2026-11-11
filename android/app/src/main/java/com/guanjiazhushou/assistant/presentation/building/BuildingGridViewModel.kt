package com.guanjiazhushou.assistant.presentation.building

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guanjiazhushou.assistant.domain.model.BatchRoomUpdateParams
import com.guanjiazhushou.assistant.domain.model.Building
import com.guanjiazhushou.assistant.domain.model.BuildingMetrics
import com.guanjiazhushou.assistant.domain.model.FeatureTag
import com.guanjiazhushou.assistant.domain.model.LivingStatusSetting
import com.guanjiazhushou.assistant.domain.model.Room
import com.guanjiazhushou.assistant.domain.model.RoomWithFeatureIds
import com.guanjiazhushou.assistant.domain.repository.BuildingRepository
import com.guanjiazhushou.assistant.domain.repository.FeatureTagRepository
import com.guanjiazhushou.assistant.domain.repository.RoomRepository
import com.guanjiazhushou.assistant.domain.repository.SettingsRepository
import com.guanjiazhushou.assistant.domain.repository.StatisticsRepository
import com.guanjiazhushou.assistant.domain.model.PaymentType
import com.guanjiazhushou.assistant.domain.model.RoomEventSource
import com.guanjiazhushou.assistant.domain.model.RoomEventType
import com.guanjiazhushou.assistant.domain.usecase.AddBottomShopUseCase
import com.guanjiazhushou.assistant.domain.usecase.ApplyListenPaymentProposalUseCase
import com.guanjiazhushou.assistant.domain.usecase.BatchDeleteRoomsUseCase
import com.guanjiazhushou.assistant.domain.usecase.BatchUpdateRoomsUseCase
import com.guanjiazhushou.assistant.domain.model.ReminderTargetType
import com.guanjiazhushou.assistant.domain.usecase.CreateRoomEventUseCase
import com.guanjiazhushou.assistant.domain.usecase.RemoveBottomShopUseCase
import com.guanjiazhushou.assistant.domain.usecase.ScheduleReminderUseCase
import com.guanjiazhushou.assistant.domain.usecase.UpdateRoomUseCase
import com.guanjiazhushou.assistant.domain.voice.CandidateAction
import com.guanjiazhushou.assistant.domain.voice.ListenSessionState
import com.guanjiazhushou.assistant.presentation.navigation.Routes
import com.guanjiazhushou.assistant.presentation.navigation.getNavLong
import com.guanjiazhushou.assistant.presentation.voice.ListenSessionController
import com.guanjiazhushou.assistant.presentation.voice.ListenUiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class BuildingGridUiState(
    val building: Building? = null,
    val rooms: List<RoomWithFeatureIds> = emptyList(),
    val livingStatusSettings: List<LivingStatusSetting> = emptyList(),
    val featureTags: Map<Long, FeatureTag> = emptyMap(),
    val metrics: BuildingMetrics = BuildingMetrics(),
    val selectedRoom: Room? = null,
    val selectedFeatureTagIds: List<Long> = emptyList(),
    val isSelectionMode: Boolean = false,
    val selectedRoomIds: Set<Long> = emptySet(),
    val showBatchSettingsDialog: Boolean = false,
    val showAddBottomShopDialog: Boolean = false,
    val operationError: String? = null,
)

@HiltViewModel
class BuildingGridViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val buildingRepository: BuildingRepository,
    private val roomRepository: RoomRepository,
    private val statisticsRepository: StatisticsRepository,
    private val featureTagRepository: FeatureTagRepository,
    private val settingsRepository: SettingsRepository,
    private val updateRoomUseCase: UpdateRoomUseCase,
    private val batchUpdateRoomsUseCase: BatchUpdateRoomsUseCase,
    private val batchDeleteRoomsUseCase: BatchDeleteRoomsUseCase,
    private val addBottomShopUseCase: AddBottomShopUseCase,
    private val removeBottomShopUseCase: RemoveBottomShopUseCase,
    private val listenSessionController: ListenSessionController,
    private val applyListenPaymentProposalUseCase: ApplyListenPaymentProposalUseCase,
    private val createRoomEventUseCase: CreateRoomEventUseCase,
    private val scheduleReminderUseCase: ScheduleReminderUseCase,
) : ViewModel() {

    val listenState: StateFlow<ListenSessionState> = listenSessionController.state
    val listenUiEvent: StateFlow<ListenUiEvent> = listenSessionController.uiEvent

    private val buildingId: Long = checkNotNull(savedStateHandle.getNavLong("buildingId"))
    private val highlightRoomId: Long? = savedStateHandle.getNavLong("roomId")
        ?.takeIf { it != Routes.NO_ROOM_HIGHLIGHT }

    private val _metrics = MutableStateFlow(BuildingMetrics())
    private val _selectedRoom = MutableStateFlow<Room?>(null)
    private val _selectedFeatureTagIds = MutableStateFlow<List<Long>>(emptyList())
    private val _isSelectionMode = MutableStateFlow(false)
    private val _selectedRoomIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _showBatchSettingsDialog = MutableStateFlow(false)
    private val _showAddBottomShopDialog = MutableStateFlow(false)
    private val _operationError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BuildingGridUiState> = combine(
        combine(
            buildingRepository.observeAll(),
            roomRepository.observeWithFeatures(buildingId),
            featureTagRepository.observeAll(),
            settingsRepository.observeLivingStatusSettings(),
        ) { buildings, rooms, featureTagList, livingStatusSettings ->
            Triple(buildings, rooms, featureTagList to livingStatusSettings)
        },
        combine(
            _metrics,
            _selectedRoom,
            _selectedFeatureTagIds,
            _isSelectionMode,
            _selectedRoomIds,
            _showBatchSettingsDialog,
            _showAddBottomShopDialog,
            _operationError,
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            SelectionBundle(
                metrics = values[0] as BuildingMetrics,
                selectedRoom = values[1] as Room?,
                selectedTags = values[2] as List<Long>,
                isSelectionMode = values[3] as Boolean,
                selectedRoomIds = values[4] as Set<Long>,
                showBatchDialog = values[5] as Boolean,
                showAddShopDialog = values[6] as Boolean,
                operationError = values[7] as String?,
            )
        },
    ) { (buildings, rooms, featureData), selection ->
        val (featureTagList, livingStatusSettings) = featureData
        BuildingGridUiState(
            building = buildings.firstOrNull { it.id == buildingId },
            rooms = rooms,
            livingStatusSettings = livingStatusSettings,
            featureTags = featureTagList.associateBy { it.id },
            metrics = selection.metrics,
            selectedRoom = selection.selectedRoom,
            selectedFeatureTagIds = selection.selectedTags,
            isSelectionMode = selection.isSelectionMode,
            selectedRoomIds = selection.selectedRoomIds,
            showBatchSettingsDialog = selection.showBatchDialog,
            showAddBottomShopDialog = selection.showAddShopDialog,
            operationError = selection.operationError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BuildingGridUiState(),
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            refreshMetrics()
        }
        viewModelScope.launch {
            combine(
                roomRepository.observeWithFeatures(buildingId),
                featureTagRepository.observeAll(),
            ) { rooms, tags -> rooms to tags }
                .collect { (rooms, tags) ->
                    val listening = listenSessionController.state.value
                    if (listening.buildingId == buildingId && listening.isListening) {
                        listenSessionController.refreshRooms(rooms, tags)
                    } else if (!listening.isListening || listening.buildingId == 0L) {
                        listenSessionController.enterBuilding(buildingId, rooms, tags)
                    }
                }
        }
        highlightRoomId?.let { roomId ->
            viewModelScope.launch {
                val roomWithFeatures = roomRepository.observeWithFeatures(buildingId)
                    .first { rooms -> rooms.any { it.room.id == roomId } }
                    .firstOrNull { it.room.id == roomId }
                roomWithFeatures?.let { selectRoom(it) }
            }
        }
    }

    override fun onCleared() {
        listenSessionController.leaveBuilding()
        super.onCleared()
    }

    fun toggleListenMute() {
        val muted = listenSessionController.state.value.isMuted
        listenSessionController.setMuted(!muted)
    }

    fun processListenText(text: String) {
        listenSessionController.processTranscript(text)
    }

    fun clearListenUiEvent() {
        listenSessionController.clearUiEvent()
    }

    fun consumeListenNavigation() {
        listenSessionController.consumeNavigation()
    }

    fun consumeListenProposals() {
        listenSessionController.consumeProposals()
    }

    fun consumeListenAmbiguous() {
        listenSessionController.consumeAmbiguousRooms()
    }

    fun clearListenError() {
        listenSessionController.clearError()
    }

    /** Re-attempt mic ASR after RECORD_AUDIO is granted. */
    fun retryListenAsr() {
        listenSessionController.retryAsr()
    }

    fun resolveRoomId(roomNumber: String?): Long? {
        if (roomNumber == null) {
            return listenSessionController.state.value.focusedRoomId
                ?: _selectedRoom.value?.id
        }
        return listenSessionController.state.value.roomNumberToId[roomNumber]
            ?: uiState.value.rooms.firstOrNull { it.room.roomNumber == roomNumber }?.room?.id
    }

    fun confirmPaymentProposal(
        proposal: CandidateAction.ProposePayment,
        paymentType: PaymentType,
        months: Int,
    ) {
        val roomId = resolveRoomId(proposal.roomNumber) ?: run {
            _operationError.value = "未定位到房屋，无法写入收费"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                applyListenPaymentProposalUseCase(roomId, paymentType, months)
                refreshMetrics()
            } catch (e: Exception) {
                _operationError.value = e.message ?: "收费写入失败"
            }
        }
    }

    fun confirmEventProposal(proposal: CandidateAction.ProposeEvent) {
        val roomId = resolveRoomId(proposal.roomNumber) ?: run {
            _operationError.value = "未定位到房屋，无法创建事件"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                createRoomEventUseCase(
                    buildingId = buildingId,
                    roomId = roomId,
                    type = RoomEventType.fromKey(proposal.eventType),
                    summary = proposal.summary,
                    rawText = proposal.rawText,
                    source = RoomEventSource.LISTEN,
                )
            } catch (e: Exception) {
                _operationError.value = e.message ?: "事件写入失败"
            }
        }
    }

    fun schedulePaymentFollowUpReminder(room: Room, triggerAtMs: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                scheduleReminderUseCase(
                    targetType = ReminderTargetType.PAYMENT_FOLLOWUP,
                    targetId = room.id,
                    buildingId = room.buildingId,
                    roomId = room.id,
                    title = "缴费跟进 ${room.roomNumber}",
                    body = "${room.paymentType.label} ${room.paymentMonths}月，请催缴/核实",
                    triggerAt = triggerAtMs,
                )
            } catch (e: Exception) {
                _operationError.value = e.message ?: "设置缴费跟进提醒失败"
            }
        }
    }

    fun schedulePaymentFollowUpAfterListen(
        roomId: Long,
        paymentType: PaymentType,
        months: Int,
        triggerAtMs: Long,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val room = roomRepository.getById(roomId) ?: error("房间不存在")
                scheduleReminderUseCase(
                    targetType = ReminderTargetType.PAYMENT_FOLLOWUP,
                    targetId = roomId,
                    buildingId = room.buildingId,
                    roomId = roomId,
                    title = "缴费跟进 ${room.roomNumber}",
                    body = "${paymentType.label} ${months}月，请催缴/核实",
                    triggerAt = triggerAtMs,
                )
            } catch (e: Exception) {
                _operationError.value = e.message ?: "设置缴费跟进提醒失败"
            }
        }
    }

    fun confirmFeatureTagProposal(proposal: CandidateAction.ProposeFeatureTags) {
        val roomId = resolveRoomId(proposal.roomNumber) ?: run {
            _operationError.value = "未定位到房屋，无法更新特征标签"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val room = roomRepository.getById(roomId) ?: error("房间不存在")
                val current = roomRepository.getFeatureTagIdsForRoom(roomId).toMutableList()
                if (proposal.remove) {
                    current.removeAll(proposal.tagIds.toSet())
                } else {
                    proposal.tagIds.forEach { id ->
                        if (id !in current) current += id
                    }
                }
                updateRoomUseCase(room, current)
            } catch (e: Exception) {
                _operationError.value = e.message ?: "特征标签写入失败"
            }
        }
    }

    private data class SelectionBundle(
        val metrics: BuildingMetrics,
        val selectedRoom: Room?,
        val selectedTags: List<Long>,
        val isSelectionMode: Boolean,
        val selectedRoomIds: Set<Long>,
        val showBatchDialog: Boolean,
        val showAddShopDialog: Boolean,
        val operationError: String?,
    )

    private suspend fun refreshMetrics() {
        _metrics.value = statisticsRepository.getBuildingMetrics(buildingId)
    }

    fun onRoomClick(roomWithFeatures: RoomWithFeatureIds) {
        if (_isSelectionMode.value) {
            toggleRoomSelection(roomWithFeatures.room.id)
        } else {
            selectRoom(roomWithFeatures)
        }
    }

    fun onRoomLongClick(roomWithFeatures: RoomWithFeatureIds) {
        if (uiState.value.building?.isReadonly == true) return
        startDragSelection(roomWithFeatures.room.id)
    }

    fun startDragSelection(roomId: Long) {
        if (uiState.value.building?.isReadonly == true) return
        _isSelectionMode.value = true
        _selectedRoom.value = null
        _selectedFeatureTagIds.value = emptyList()
        _selectedRoomIds.update { it + roomId }
    }

    fun addRoomToSelection(roomId: Long) {
        if (!_isSelectionMode.value) return
        _selectedRoomIds.update { it + roomId }
    }

    private fun toggleRoomSelection(roomId: Long) {
        _selectedRoomIds.update { current ->
            if (roomId in current) current - roomId else current + roomId
        }
    }

    fun selectRoom(roomWithFeatures: RoomWithFeatureIds) {
        _selectedRoom.value = roomWithFeatures.room
        _selectedFeatureTagIds.value = roomWithFeatures.featureTagIds
        listenSessionController.updateFocus(roomWithFeatures.room.id, roomWithFeatures.room.roomNumber)
    }

    fun dismissRoomDetail() {
        _selectedRoom.value = null
        _selectedFeatureTagIds.value = emptyList()
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedRoomIds.value = emptySet()
        _showBatchSettingsDialog.value = false
    }

    fun openBatchSettingsDialog() {
        if (_selectedRoomIds.value.isNotEmpty()) {
            _showBatchSettingsDialog.value = true
        }
    }

    fun dismissBatchSettingsDialog() {
        _showBatchSettingsDialog.value = false
    }

    fun applyBatchSettings(params: BatchRoomUpdateParams) {
        val roomIds = _selectedRoomIds.value.toList()
        if (roomIds.isEmpty() || !params.isValid) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _operationError.value = null
                batchUpdateRoomsUseCase(
                    buildingId = buildingId,
                    roomIds = roomIds,
                    params = params,
                )
                refreshMetrics()
                withContext(Dispatchers.Main) {
                    exitSelectionMode()
                }
            } catch (e: Exception) {
                _operationError.value = e.message ?: "批量设置失败"
            }
        }
    }

    fun batchDeleteSelectedRooms() {
        val roomIds = _selectedRoomIds.value.toList()
        if (roomIds.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _operationError.value = null
                batchDeleteRoomsUseCase(buildingId, roomIds)
                refreshMetrics()
                withContext(Dispatchers.Main) {
                    exitSelectionMode()
                }
            } catch (e: Exception) {
                _operationError.value = e.message ?: "批量删除失败"
            }
        }
    }

    fun updateSelectedRoom(room: Room, featureTagIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _operationError.value = null
                updateRoomUseCase(room, featureTagIds)
                refreshMetrics()
                withContext(Dispatchers.Main) {
                    dismissRoomDetail()
                }
            } catch (e: Exception) {
                _operationError.value = e.message ?: "保存失败"
            }
        }
    }

    fun openAddBottomShopDialog() {
        _operationError.value = null
        _showAddBottomShopDialog.value = true
    }

    fun dismissAddBottomShopDialog() {
        _showAddBottomShopDialog.value = false
        _operationError.value = null
    }

    fun addBottomShop(roomNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _operationError.value = null
                addBottomShopUseCase(buildingId, roomNumber)
                refreshMetrics()
                withContext(Dispatchers.Main) {
                    _showAddBottomShopDialog.value = false
                }
            } catch (e: Exception) {
                _operationError.value = e.message ?: "新增铺位失败"
            }
        }
    }

    fun removeBottomShop(roomId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _operationError.value = null
                removeBottomShopUseCase(buildingId, roomId)
                refreshMetrics()
                withContext(Dispatchers.Main) {
                    dismissRoomDetail()
                }
            } catch (e: Exception) {
                _operationError.value = e.message ?: "删除铺位失败"
            }
        }
    }

    fun dismissOperationError() {
        _operationError.value = null
    }

    fun toggleFeatureTag(tagId: Long) {
        _selectedFeatureTagIds.update { current ->
            if (tagId in current) current - tagId else current + tagId
        }
    }
}
