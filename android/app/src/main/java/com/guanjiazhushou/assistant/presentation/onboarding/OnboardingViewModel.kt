package com.guanjiazhushou.assistant.presentation.onboarding

import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guanjiazhushou.assistant.domain.model.OnboardingStep
import com.guanjiazhushou.assistant.domain.repository.BuildingRepository
import com.guanjiazhushou.assistant.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val buildingRepository: BuildingRepository,
) : ViewModel() {

    private val _currentStep = MutableStateFlow<OnboardingStep?>(null)
    val currentStep: StateFlow<OnboardingStep?> = _currentStep.asStateFlow()

    private val _anchors = MutableStateFlow<Map<OnboardingStep, Rect>>(emptyMap())
    val anchors: StateFlow<Map<OnboardingStep, Rect>> = _anchors.asStateFlow()

    private var initialized = false

    fun ensureInitialized() {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            when (val stored = settingsRepository.getPreference(PREF_ONBOARDING_STEP)) {
                PREF_DONE -> _currentStep.value = null
                null -> {
                    val hasBuildings = buildingRepository.observeAll().first().isNotEmpty()
                    if (hasBuildings) {
                        completeOnboarding()
                    } else {
                        _currentStep.value = OnboardingStep.CREATE_BUILDING
                        persistStep(OnboardingStep.CREATE_BUILDING)
                    }
                }
                else -> {
                    _currentStep.value = OnboardingStep.fromStored(stored)
                }
            }
        }
    }

    fun registerAnchor(step: OnboardingStep, bounds: Rect) {
        _anchors.update { current ->
            current + (step to bounds)
        }
    }

    fun onCreateBuildingClicked() {
        if (_currentStep.value == OnboardingStep.CREATE_BUILDING) {
            advanceTo(OnboardingStep.FILL_FORM)
        }
    }

    /** @return true when onboarding advanced to step 3 and caller should stay on building list */
    fun onBuildingSaved(): Boolean {
        if (_currentStep.value == OnboardingStep.FILL_FORM) {
            advanceTo(OnboardingStep.MODIFY_DELETE)
            return true
        }
        return false
    }

    fun advanceFromCurrentStep() {
        when (_currentStep.value) {
            OnboardingStep.CREATE_BUILDING -> advanceTo(OnboardingStep.FILL_FORM)
            OnboardingStep.FILL_FORM -> advanceTo(OnboardingStep.MODIFY_DELETE)
            OnboardingStep.MODIFY_DELETE -> advanceTo(OnboardingStep.STATISTICS)
            OnboardingStep.STATISTICS -> completeOnboarding()
            null -> Unit
        }
    }

    fun skipOnboarding() {
        completeOnboarding()
    }

    private fun advanceTo(step: OnboardingStep) {
        _currentStep.value = step
        viewModelScope.launch {
            persistStep(step)
        }
    }

    private fun completeOnboarding() {
        _currentStep.value = null
        viewModelScope.launch {
            settingsRepository.setPreference(PREF_ONBOARDING_STEP, PREF_DONE)
        }
    }

    private suspend fun persistStep(step: OnboardingStep) {
        settingsRepository.setPreference(PREF_ONBOARDING_STEP, step.stepNumber.toString())
    }

    companion object {
        const val PREF_ONBOARDING_STEP = "onboarding_step"
        const val PREF_DONE = "done"
    }
}
