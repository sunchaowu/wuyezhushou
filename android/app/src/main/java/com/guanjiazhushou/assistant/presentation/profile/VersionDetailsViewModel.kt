package com.guanjiazhushou.assistant.presentation.profile

import androidx.lifecycle.ViewModel
import com.guanjiazhushou.assistant.data.appinfo.AppVersionInfo
import com.guanjiazhushou.assistant.data.appinfo.AppVersionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class VersionDetailsUiState(
    val appInfo: AppVersionInfo,
    val thirdPartyLicenses: String,
)

@HiltViewModel
class VersionDetailsViewModel @Inject constructor(
    appVersionRepository: AppVersionRepository,
) : ViewModel() {

    val uiState: VersionDetailsUiState = VersionDetailsUiState(
        appInfo = appVersionRepository.getAppVersionInfo(),
        thirdPartyLicenses = appVersionRepository.loadThirdPartyLicenses(),
    )
}
