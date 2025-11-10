package com.voxaid.feature.main.loading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voxaid.core.common.datastore.PreferencesManager
import com.voxaid.core.common.model.UpdateCheckResult
import com.voxaid.core.common.model.UpdateInfo
import com.voxaid.core.common.network.UpdateCheckService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

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
        initLoadingSequence()
    }

    private fun initLoadingSequence() {
        viewModelScope.launch {
            try {
                // Safely get disclaimer state
                val accepted = withContext(Dispatchers.IO) {
                    preferencesManager.disclaimerAccepted.firstOrNull() ?: false
                }
                _disclaimerAccepted.value = accepted

                if (!accepted) {
                    Timber.d("Disclaimer not accepted - showing disclaimer")
                    _uiState.value = LoadingUiState.ShowDisclaimer
                    return@launch
                }

                // Minimum branding delay
                delay(1000)

                checkForUpdates()
            } catch (e: Exception) {
                Timber.e(e, "Error during loading sequence")
                _uiState.value = LoadingUiState.ReadyToProceed
            }
        }
    }

    fun acceptDisclaimer() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    preferencesManager.setDisclaimerAccepted(true)
                }
                _disclaimerAccepted.value = true
                _uiState.value = LoadingUiState.Loading

                // Short delay to allow UI transition
                delay(500)
                checkForUpdates()
            } catch (e: Exception) {
                Timber.e(e, "Error accepting disclaimer")
                _uiState.value = LoadingUiState.ReadyToProceed
            }
        }
    }

    private fun checkForUpdates() {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    updateCheckService.checkForUpdate()
                }

                when (result) {
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
                        _uiState.value = LoadingUiState.ReadyToProceed
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during update check")
                _uiState.value = LoadingUiState.ReadyToProceed
            }
        }
    }

    fun dismissUpdate() {
        _uiState.value = LoadingUiState.ReadyToProceed
    }

    fun retryLoading() {
        _uiState.value = LoadingUiState.Loading
        initLoadingSequence()
    }
}

// --- UI state ---
sealed class LoadingUiState {
    data object Loading : LoadingUiState()
    data object ShowDisclaimer : LoadingUiState()
    data class UpdateAvailable(val updateInfo: UpdateInfo) : LoadingUiState()
    data object ReadyToProceed : LoadingUiState()
    data class Error(val message: String) : LoadingUiState()
}