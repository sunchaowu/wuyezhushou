package com.guanjiazhushou.assistant.presentation.building

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guanjiazhushou.assistant.domain.model.BuildingKind
import com.guanjiazhushou.assistant.domain.model.PaymentType
import com.guanjiazhushou.assistant.domain.util.RoomGenerator
import com.guanjiazhushou.assistant.domain.repository.BuildingRepository
import com.guanjiazhushou.assistant.domain.repository.CommunityRepository
import com.guanjiazhushou.assistant.domain.repository.RoomRepository
import com.guanjiazhushou.assistant.domain.usecase.CreateBuildingParams
import com.guanjiazhushou.assistant.domain.usecase.CreateBuildingUseCase
import com.guanjiazhushou.assistant.domain.usecase.GenerateRoomsUseCase
import com.guanjiazhushou.assistant.domain.usecase.UpdateBuildingUseCase
import com.guanjiazhushou.assistant.presentation.navigation.getNavLong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BuildingFormSnapshot(
    val buildingKind: BuildingKind,
    val communityName: String,
    val name: String,
    val unitsPerFloor: Int,
    val maxFloor: Int,
    val startFloor: Int,
    val refugeFloors: String,
    val excludeRooms: String,
    val shopRooms: String,
)

data class BuildingSaveChangeSummary(
    val paramChanges: List<String>,
    val addedRoomNumbers: List<String>,
    val removedRoomNumbers: List<String>,
) {
    fun toMessage(): String = buildString {
        if (paramChanges.isNotEmpty()) {
            appendLine("参数变更：")
            paramChanges.forEach { appendLine("· $it") }
        }
        if (addedRoomNumbers.isNotEmpty()) {
            appendLine("将新增 ${addedRoomNumbers.size} 间房屋：${formatRoomList(addedRoomNumbers)}")
        }
        if (removedRoomNumbers.isNotEmpty()) {
            appendLine("将删除 ${removedRoomNumbers.size} 间房屋及关联数据：${formatRoomList(removedRoomNumbers)}")
        }
        if (paramChanges.isEmpty() && addedRoomNumbers.isEmpty() && removedRoomNumbers.isEmpty()) {
            append("未检测到房间增减，仅更新楼栋信息。")
        }
    }

    private fun formatRoomList(rooms: List<String>): String =
        when {
            rooms.size <= 6 -> rooms.joinToString("、")
            else -> rooms.take(6).joinToString("、") + " 等"
        }
}

data class BuildingFormUiState(
    val buildingKind: BuildingKind = BuildingKind.RESIDENTIAL,
    val communityName: String = "",
    val name: String = "",
    val unitsPerFloor: String = "4",
    val maxFloor: String = "18",
    val startFloor: String = "1",
    val refugeFloors: String = "",
    val excludeRooms: String = "",
    val shopRooms: String = "",
    val defaultPaymentType: PaymentType = PaymentType.ARREARS,
    val isLoading: Boolean = false,
    val isReadonly: Boolean = false,
    val isEditMode: Boolean = false,
    val errorMessage: String? = null,
    val pendingSaveSummary: BuildingSaveChangeSummary? = null,
)

@HiltViewModel
class BuildingFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val createBuildingUseCase: CreateBuildingUseCase,
    private val updateBuildingUseCase: UpdateBuildingUseCase,
    private val generateRoomsUseCase: GenerateRoomsUseCase,
    private val buildingRepository: BuildingRepository,
    private val roomRepository: RoomRepository,
    private val communityRepository: CommunityRepository,
) : ViewModel() {

    private val buildingId: Long? = savedStateHandle.getNavLong("buildingId")
    private var originalSnapshot: BuildingFormSnapshot? = null
    private var shopRoomsManuallyEdited: Boolean = false

    private val _uiState = MutableStateFlow(BuildingFormUiState())
    val uiState: StateFlow<BuildingFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            buildingId?.let { id ->
                buildingRepository.getById(id)?.let { building ->
                    val community = communityRepository.getById(building.communityId)
                        ?: communityRepository.getOrCreateDefault()
                    shopRoomsManuallyEdited = true
                    originalSnapshot = BuildingFormSnapshot(
                        buildingKind = building.buildingKind,
                        communityName = community.name,
                        name = building.name,
                        unitsPerFloor = building.unitsPerFloor,
                        maxFloor = building.maxFloor,
                        startFloor = building.startFloor,
                        refugeFloors = building.refugeFloors,
                        excludeRooms = building.excludeRooms,
                        shopRooms = building.shopRooms,
                    )
                    _uiState.update {
                        it.copy(
                            buildingKind = building.buildingKind,
                            communityName = community.name,
                            name = building.name,
                            unitsPerFloor = building.unitsPerFloor.toString(),
                            maxFloor = building.maxFloor.toString(),
                            startFloor = building.startFloor.toString(),
                            refugeFloors = building.refugeFloors,
                            excludeRooms = building.excludeRooms,
                            shopRooms = building.shopRooms,
                            isReadonly = building.isReadonly,
                            isEditMode = true,
                        )
                    }
                }
            } ?: run {
                _uiState.update { it.copy(communityName = "") }
            }
        }
    }

    fun updateBuildingKind(kind: BuildingKind) {
        shopRoomsManuallyEdited = false
        _uiState.update { it.copy(buildingKind = kind) }
        refreshShopRoomsPreview()
    }

    fun updateCommunityName(value: String) = _uiState.update { it.copy(communityName = value) }
    fun updateName(value: String) = _uiState.update { it.copy(name = value) }
    fun updateUnitsPerFloor(value: String) {
        _uiState.update { it.copy(unitsPerFloor = value) }
        refreshShopRoomsPreview()
    }
    fun updateMaxFloor(value: String) {
        _uiState.update { it.copy(maxFloor = value) }
        refreshShopRoomsPreview()
    }
    fun updateStartFloor(value: String) {
        _uiState.update { it.copy(startFloor = value) }
        refreshShopRoomsPreview()
    }
    fun updateRefugeFloors(value: String) {
        _uiState.update { it.copy(refugeFloors = value) }
        refreshShopRoomsPreview()
    }
    fun updateExcludeRooms(value: String) {
        _uiState.update { it.copy(excludeRooms = value) }
        refreshShopRoomsPreview()
    }
    fun updateShopRooms(value: String) {
        shopRoomsManuallyEdited = true
        _uiState.update { it.copy(shopRooms = value) }
    }

    private fun refreshShopRoomsPreview() {
        if (shopRoomsManuallyEdited || _uiState.value.buildingKind != BuildingKind.SHOP) return
        val units = _uiState.value.unitsPerFloor.toIntOrNull() ?: return
        val maxFloor = _uiState.value.maxFloor.toIntOrNull() ?: return
        val startFloor = _uiState.value.startFloor.toIntOrNull() ?: return
        if (startFloor > maxFloor) return
        val preview = RoomGenerator.generateDefaultShopRoomNumbers(
            startFloor = startFloor,
            maxFloor = maxFloor,
            unitsPerFloor = units,
            refugeFloors = _uiState.value.refugeFloors.trim(),
            excludeRooms = _uiState.value.excludeRooms.trim(),
        )
        _uiState.update { it.copy(shopRooms = preview) }
    }
    fun updateDefaultPaymentType(type: PaymentType) = _uiState.update { it.copy(defaultPaymentType = type) }

    fun dismissSaveConfirmation() = _uiState.update { it.copy(pendingSaveSummary = null) }

    fun requestSave(onSuccess: (Long) -> Unit) {
        val parsed = validateForm() ?: return
        val state = _uiState.value
        if (state.isEditMode && buildingId != null) {
            viewModelScope.launch {
                val summary = buildChangeSummary(buildingId, parsed)
                _uiState.update { it.copy(pendingSaveSummary = summary) }
            }
            pendingSuccessCallback = onSuccess
        } else {
            performSave(onSuccess, parsed)
        }
    }

    fun confirmSave() {
        val callback = pendingSuccessCallback ?: return
        val parsed = validateForm() ?: return
        _uiState.update { it.copy(pendingSaveSummary = null) }
        performSave(callback, parsed)
    }

    private var pendingSuccessCallback: ((Long) -> Unit)? = null

    private data class ParsedForm(
        val communityName: String,
        val name: String,
        val units: Int,
        val maxFloor: Int,
        val startFloor: Int,
        val refugeFloors: String,
        val excludeRooms: String,
        val shopRooms: String,
    )

    private fun validateForm(): ParsedForm? {
        val state = _uiState.value
        if (state.communityName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入小区名称") }
            return null
        }
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入楼栋名称") }
            return null
        }
        val units = state.unitsPerFloor.toIntOrNull()
        val maxFloor = state.maxFloor.toIntOrNull()
        val startFloor = state.startFloor.toIntOrNull()
        if (units == null || maxFloor == null || startFloor == null) {
            _uiState.update { it.copy(errorMessage = "请填写有效的楼层参数") }
            return null
        }
        if (startFloor > maxFloor) {
            _uiState.update { it.copy(errorMessage = "起始楼层不能高于最高楼层") }
            return null
        }
        return ParsedForm(
            communityName = state.communityName.trim(),
            name = state.name.trim(),
            units = units,
            maxFloor = maxFloor,
            startFloor = startFloor,
            refugeFloors = state.refugeFloors.trim(),
            excludeRooms = state.excludeRooms.trim(),
            shopRooms = state.shopRooms.trim(),
        )
    }

    private suspend fun buildChangeSummary(
        id: Long,
        parsed: ParsedForm,
    ): BuildingSaveChangeSummary {
        val existingNumbers = roomRepository.getByBuilding(id).map { it.roomNumber }.toSet()
        val buildingKind = _uiState.value.buildingKind
        val generatedNumbers = generateRoomsUseCase(
            buildingId = id,
            buildingKind = buildingKind,
            startFloor = parsed.startFloor,
            maxFloor = parsed.maxFloor,
            unitsPerFloor = parsed.units,
            refugeFloors = parsed.refugeFloors,
            excludeRooms = parsed.excludeRooms,
            shopRooms = parsed.shopRooms,
            defaultPaymentType = _uiState.value.defaultPaymentType,
        ).map { it.roomNumber }.toSet()

        val original = originalSnapshot
        val paramChanges = buildList {
            if (original == null) return@buildList
            if (original.communityName != parsed.communityName) {
                add("小区：${original.communityName} → ${parsed.communityName}")
            }
            if (original.name != parsed.name) {
                add("楼栋名称：${original.name} → ${parsed.name}")
            }
            if (original.buildingKind != buildingKind) {
                add("楼栋类型：${original.buildingKind.label} → ${buildingKind.label}")
            }
            if (original.startFloor != parsed.startFloor) {
                add("起始楼层：${original.startFloor} → ${parsed.startFloor}")
            }
            if (original.maxFloor != parsed.maxFloor) {
                add("最高楼层：${original.maxFloor} → ${parsed.maxFloor}")
            }
            if (original.unitsPerFloor != parsed.units) {
                add("每层户数：${original.unitsPerFloor} → ${parsed.units}")
            }
            if (original.refugeFloors != parsed.refugeFloors) {
                add("避难层已调整")
            }
            if (original.excludeRooms != parsed.excludeRooms) {
                add("剔除房间已调整")
            }
            if (original.shopRooms != parsed.shopRooms) {
                add(
                    if (buildingKind == BuildingKind.SHOP) "商铺编号已调整" else "商铺房间已调整",
                )
            }
        }

        return BuildingSaveChangeSummary(
            paramChanges = paramChanges,
            addedRoomNumbers = (generatedNumbers - existingNumbers).sorted(),
            removedRoomNumbers = (existingNumbers - generatedNumbers).sorted(),
        )
    }

    private fun performSave(onSuccess: (Long) -> Unit, parsed: ParsedForm) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val community = communityRepository.getOrCreateByName(parsed.communityName)
                val defaultPayment = _uiState.value.defaultPaymentType
                val buildingKind = _uiState.value.buildingKind
                val id = if (buildingId != null) {
                    val existing = buildingRepository.getById(buildingId)
                        ?: throw IllegalStateException("楼栋不存在")
                    updateBuildingUseCase(
                        building = existing.copy(
                            communityId = community.id,
                            name = parsed.name,
                            buildingKind = buildingKind,
                            unitsPerFloor = parsed.units,
                            maxFloor = parsed.maxFloor,
                            startFloor = parsed.startFloor,
                            refugeFloors = parsed.refugeFloors,
                            excludeRooms = parsed.excludeRooms,
                            shopRooms = parsed.shopRooms,
                            updatedAt = System.currentTimeMillis(),
                        ),
                        defaultPaymentType = defaultPayment,
                    )
                    buildingId
                } else {
                    createBuildingUseCase(
                        CreateBuildingParams(
                            communityId = community.id,
                            name = parsed.name,
                            buildingKind = buildingKind,
                            unitsPerFloor = parsed.units,
                            maxFloor = parsed.maxFloor,
                            startFloor = parsed.startFloor,
                            refugeFloors = parsed.refugeFloors,
                            excludeRooms = parsed.excludeRooms,
                            shopRooms = parsed.shopRooms,
                            defaultPaymentType = defaultPayment,
                        ),
                    )
                }
                _uiState.update { it.copy(isLoading = false, pendingSaveSummary = null) }
                pendingSuccessCallback = null
                onSuccess(id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "保存失败")
                }
            }
        }
    }
}
