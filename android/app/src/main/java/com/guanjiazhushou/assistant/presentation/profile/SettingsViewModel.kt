package com.guanjiazhushou.assistant.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guanjiazhushou.assistant.domain.model.FeatureTag
import com.guanjiazhushou.assistant.domain.model.IconType
import com.guanjiazhushou.assistant.domain.model.LivingStatusSetting
import com.guanjiazhushou.assistant.domain.repository.FeatureTagRepository
import com.guanjiazhushou.assistant.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SettingsTab(val label: String) {
    LIVING_STATUS("居住状态"),
    FEATURE_PROFILE("特征画像"),
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val featureTagRepository: FeatureTagRepository,
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(SettingsTab.LIVING_STATUS)
    val selectedTab: StateFlow<SettingsTab> = _selectedTab.asStateFlow()

    private val _featureMessage = MutableStateFlow<String?>(null)
    val featureMessage: StateFlow<String?> = _featureMessage.asStateFlow()

    val livingSettings: StateFlow<List<LivingStatusSetting>> =
        settingsRepository.observeLivingStatusSettings()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    val featureTags: StateFlow<List<FeatureTag>> =
        featureTagRepository.observeAll()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            featureTagRepository.ensureBuiltinTags()
        }
    }

    fun selectTab(tab: SettingsTab) {
        _selectedTab.value = tab
    }

    fun clearFeatureMessage() {
        _featureMessage.value = null
    }

    fun toggleOccupied(statusKey: String, countsAsOccupied: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.updateCountsAsOccupied(statusKey, countsAsOccupied)
        }
    }

    fun addLivingStatus(
        label: String,
        colorArgb: Long,
        countsAsOccupied: Boolean = false,
    ) {
        val trimmedLabel = label.trim()
        if (trimmedLabel.isEmpty()) {
            _featureMessage.value = "请输入居住状态名称"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            if (settingsRepository.existsByLabel(trimmedLabel)) {
                _featureMessage.update { "居住状态「$trimmedLabel」已存在" }
                return@launch
            }
            val nextSort = settingsRepository.getLivingStatusSettings()
                .maxOfOrNull { it.sortOrder }
                ?.plus(1) ?: 0
            val statusKey = "custom_${System.currentTimeMillis()}"
            settingsRepository.insertLivingStatus(
                LivingStatusSetting(
                    statusKey = statusKey,
                    label = trimmedLabel,
                    colorArgb = colorArgb,
                    countsAsOccupied = countsAsOccupied,
                    isBuiltin = false,
                    sortOrder = nextSort,
                ),
            )
            _featureMessage.update { null }
        }
    }

    fun updateLivingStatus(
        setting: LivingStatusSetting,
        label: String,
        colorArgb: Long,
        countsAsOccupied: Boolean,
    ) {
        val trimmedLabel = label.trim()
        if (trimmedLabel.isEmpty()) {
            _featureMessage.value = "请输入居住状态名称"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            if (settingsRepository.existsByLabel(trimmedLabel, setting.statusKey)) {
                _featureMessage.update { "居住状态「$trimmedLabel」已存在" }
                return@launch
            }
            settingsRepository.updateLivingStatus(
                setting.copy(
                    label = trimmedLabel,
                    colorArgb = colorArgb,
                    countsAsOccupied = countsAsOccupied,
                ),
            )
            _featureMessage.update { null }
        }
    }

    fun deleteLivingStatus(setting: LivingStatusSetting) {
        if (setting.isBuiltin) return
        viewModelScope.launch(Dispatchers.IO) {
            val roomCount = settingsRepository.countRoomsUsingStatus(setting.statusKey)
            if (roomCount > 0) {
                _featureMessage.update {
                    "已有 $roomCount 户房屋使用「${setting.label}」，无法删除"
                }
                return@launch
            }
            val deleted = settingsRepository.deleteCustomLivingStatus(setting.statusKey)
            if (!deleted) {
                _featureMessage.update { "系统预设居住状态不可删除" }
            }
        }
    }

    fun addFeatureTag(name: String, emoji: String, description: String = "") {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            _featureMessage.value = "请输入特征名称"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            if (featureTagRepository.existsByName(trimmedName)) {
                _featureMessage.update { "特征名称「$trimmedName」已存在" }
                return@launch
            }
            val allTags = featureTagRepository.getAll()
            val nextSort = (allTags.maxOfOrNull { it.sortOrder } ?: 0) + 1
            val iconKey = emoji.trim().ifEmpty { trimmedName.take(1) }
            val trimmedDescription = description.trim().ifEmpty { null }
            featureTagRepository.insert(
                FeatureTag(
                    name = trimmedName,
                    iconType = IconType.EMOJI,
                    iconKey = iconKey,
                    isBuiltin = false,
                    sortOrder = nextSort,
                    createdAt = System.currentTimeMillis(),
                    description = trimmedDescription,
                ),
            )
            _featureMessage.update { null }
        }
    }

    fun updateFeatureTag(
        tag: FeatureTag,
        name: String,
        emoji: String,
        description: String = tag.description.orEmpty(),
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            _featureMessage.value = "请输入特征名称"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            if (featureTagRepository.existsByName(trimmedName, tag.id)) {
                _featureMessage.update { "特征名称「$trimmedName」已存在" }
                return@launch
            }
            val trimmedDescription = description.trim().ifEmpty { null }
            if (tag.isBuiltin) {
                featureTagRepository.update(
                    tag.copy(
                        name = trimmedName,
                        description = trimmedDescription,
                    ),
                )
            } else {
                val iconKey = emoji.trim().ifEmpty { trimmedName.take(1) }
                featureTagRepository.update(
                    tag.copy(
                        name = trimmedName,
                        iconType = IconType.EMOJI,
                        iconKey = iconKey,
                        description = trimmedDescription,
                    ),
                )
            }
            _featureMessage.update { null }
        }
    }

    fun deleteFeatureTag(tag: FeatureTag) {
        if (tag.isBuiltin) return
        viewModelScope.launch(Dispatchers.IO) {
            val deleted = featureTagRepository.deleteCustom(tag.id)
            if (!deleted) {
                _featureMessage.update { "内置特征不可删除" }
            }
        }
    }
}
