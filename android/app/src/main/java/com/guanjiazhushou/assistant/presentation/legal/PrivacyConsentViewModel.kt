package com.guanjiazhushou.assistant.presentation.legal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guanjiazhushou.assistant.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PrivacyConsentStatus {
    LOADING,
    REQUIRED,
    ACCEPTED,
}

data class PrivacyConsentUiState(
    val status: PrivacyConsentStatus = PrivacyConsentStatus.LOADING,
)

@HiltViewModel
class PrivacyConsentViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivacyConsentUiState())
    val uiState: StateFlow<PrivacyConsentUiState> = _uiState.asStateFlow()
    private var initialized = false

    fun ensureInitialized() {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            val acceptedVersion = settingsRepository.getPreference(PREF_PRIVACY_ACCEPTED_VERSION)
            val status = if (acceptedVersion == CURRENT_POLICY_VERSION) {
                PrivacyConsentStatus.ACCEPTED
            } else {
                PrivacyConsentStatus.REQUIRED
            }
            _uiState.update { it.copy(status = status) }
        }
    }

    fun acceptPolicy() {
        viewModelScope.launch {
            settingsRepository.setPreference(PREF_PRIVACY_ACCEPTED_VERSION, CURRENT_POLICY_VERSION)
            settingsRepository.setPreference(
                PREF_PRIVACY_ACCEPTED_AT,
                System.currentTimeMillis().toString(),
            )
            _uiState.update { it.copy(status = PrivacyConsentStatus.ACCEPTED) }
        }
    }

    companion object {
        const val CURRENT_POLICY_VERSION = "1.0.0"
        const val PREF_PRIVACY_ACCEPTED_VERSION = "privacy_policy_accepted_version"
        const val PREF_PRIVACY_ACCEPTED_AT = "privacy_policy_accepted_at"
    }
}
