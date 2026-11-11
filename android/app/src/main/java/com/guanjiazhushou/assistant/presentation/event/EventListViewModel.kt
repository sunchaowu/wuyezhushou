package com.guanjiazhushou.assistant.presentation.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guanjiazhushou.assistant.domain.model.ReminderTargetType
import com.guanjiazhushou.assistant.domain.model.RoomEvent
import com.guanjiazhushou.assistant.domain.model.RoomEventSource
import com.guanjiazhushou.assistant.domain.model.RoomEventStatus
import com.guanjiazhushou.assistant.domain.model.RoomEventType
import com.guanjiazhushou.assistant.domain.repository.RoomEventRepository
import com.guanjiazhushou.assistant.domain.usecase.CreateRoomEventUseCase
import com.guanjiazhushou.assistant.domain.usecase.ScheduleReminderUseCase
import com.guanjiazhushou.assistant.domain.usecase.UpdateRoomEventUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EventListViewModel @Inject constructor(
    private val roomEventRepository: RoomEventRepository,
    private val createRoomEventUseCase: CreateRoomEventUseCase,
    private val updateRoomEventUseCase: UpdateRoomEventUseCase,
    private val scheduleReminderUseCase: ScheduleReminderUseCase,
) : ViewModel() {
    private val _statusFilter = MutableStateFlow<RoomEventStatus?>(null)
    val statusFilter: StateFlow<RoomEventStatus?> = _statusFilter.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val events: StateFlow<List<RoomEvent>> =
        _statusFilter
            .flatMapLatest { status -> roomEventRepository.observeAll(status) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setStatusFilter(status: RoomEventStatus?) {
        _statusFilter.value = status
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setStatus(event: RoomEvent, status: RoomEventStatus) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                updateRoomEventUseCase(
                    event.copy(
                        status = status,
                        completedAt = if (status == RoomEventStatus.DONE) {
                            System.currentTimeMillis()
                        } else {
                            event.completedAt
                        },
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "更新事件状态失败"
            }
        }
    }

    fun createManual(
        buildingId: Long,
        roomId: Long,
        type: RoomEventType,
        summary: String,
    ) {
        if (buildingId <= 0L || roomId <= 0L) {
            _errorMessage.value = "请填写有效的楼栋 ID 与房屋 ID"
            return
        }
        if (summary.isBlank()) {
            _errorMessage.value = "事件摘要不能为空"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                createRoomEventUseCase(
                    buildingId = buildingId,
                    roomId = roomId,
                    type = type,
                    summary = summary,
                    source = RoomEventSource.MANUAL,
                )
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "创建事件失败"
            }
        }
    }

    fun scheduleReminder(event: RoomEvent, triggerAt: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                scheduleReminderUseCase(
                    targetType = ReminderTargetType.EVENT,
                    targetId = event.id,
                    buildingId = event.buildingId,
                    roomId = event.roomId,
                    title = "事件跟进",
                    body = event.summary,
                    triggerAt = triggerAt,
                )
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "设置提醒失败"
            }
        }
    }
}
