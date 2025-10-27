package com.voxaid.feature.main.loading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voxaid.core.common.datastore.PreferencesManager
import com.voxaid.core.common.model.UpdateCheckResult
import com.voxaid.core.common.model.UpdateInfo
import com.voxaid.core.common.network.UpdateCheckService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for loading screen.
 * Handles update checks and disclaimer state.
 */
@HiltViewModel
class LoadingViewModel @Inject constructor(
    private val updateCheckService: UpdateCheckService,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoadingUiState>(LoadingUiState.Loading)
    val uiState: StateFlow<LoadingUiState> = _uiState.asStateFlow()

    private val _disclaimerAccepted = MutableStateFlow(false)
    val disclaimerAccepted: StateFlow<Boolean> = _disclaimerAccepted.asStateFlow()

    init {
        checkDisclaimerAndUpdate()
    }

    private fun checkDisclaimerAndUpdate() {
        viewModelScope.launch {
            // Check if disclaimer was accepted
            preferencesManager.disclaimerAccepted.first().also { accepted ->
                _disclaimerAccepted.value = accepted

                if (!accepted) {
                    // Show disclaimer first
                    _uiState.value = LoadingUiState.ShowDisclaimer
                    return@launch
                }
            }

            // Minimum loading time for branding
            delay(1000)

            // Check for updates
            when (val result = updateCheckService.checkForUpdate()) {
                is UpdateCheckResult.UpdateAvailable -> {
                    Timber.i("Update available: ${result.updateInfo.latestVersion}")
                    _uiState.value = LoadingUiState.UpdateAvailable(result.updateInfo)
                }
                is UpdateCheckResult.NoUpdateNeeded -> {
                    Timber.d("No update needed")
                    _uiState.value = LoadingUiState.ReadyToProceed
                }
                is UpdateCheckResult.Error -> {
                    Timber.w("Update check error: ${result.message}")
                    // Don't block user, proceed anyway
                    _uiState.value = LoadingUiState.ReadyToProceed
                }
            }
        }
    }

    fun acceptDisclaimer() {
        viewModelScope.launch {
            preferencesManager.setDisclaimerAccepted(true)
            _disclaimerAccepted.value = true
            _uiState.value = LoadingUiState.Loading

            // After accepting, continue with update check
            delay(500)
            checkForUpdates()
        }
    }

    private fun checkForUpdates() {
        viewModelScope.launch {
            when (val result = updateCheckService.checkForUpdate()) {
                is UpdateCheckResult.UpdateAvailable -> {
                    _uiState.value = LoadingUiState.UpdateAvailable(result.updateInfo)
                }
                is UpdateCheckResult.NoUpdateNeeded -> {
                    _uiState.value = LoadingUiState.ReadyToProceed
                }
                is UpdateCheckResult.Error -> {
                    _uiState.value = LoadingUiState.ReadyToProceed
                }
            }
        }
    }

    fun dismissUpdate() {
        _uiState.value = LoadingUiState.ReadyToProceed
    }
}

/**
 * UI state for loading screen.
 */
sealed class LoadingUiState {
    data object Loading : LoadingUiState()
    data object ShowDisclaimer : LoadingUiState()
    data class UpdateAvailable(val updateInfo: UpdateInfo) : LoadingUiState()
    data object ReadyToProceed : LoadingUiState()
}