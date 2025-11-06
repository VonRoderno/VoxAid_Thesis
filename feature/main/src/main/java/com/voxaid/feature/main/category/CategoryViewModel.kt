// feature/main/src/main/java/com/voxaid/feature/main/category/CategoryViewModel.kt
package com.voxaid.feature.main.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voxaid.core.common.datastore.EmergencyLockState
import com.voxaid.core.common.datastore.EmergencyUnlockManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for category selection screen.
 * Manages emergency lock states for all protocols.
 *
 * Updated: Bandaging removed from emergency mode (not an emergency protocol)
 */
@HiltViewModel
class CategoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val emergencyUnlockManager: EmergencyUnlockManager
) : ViewModel() {

    private val mode: String = savedStateHandle.get<String>("mode") ?: "instructional"
    val isEmergencyMode = mode == "emergency"

    private val _uiState = MutableStateFlow<CategoryUiState>(CategoryUiState.Loading)
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    // Available protocols
    private val allProtocols = listOf(
        ProtocolCategory("cpr", "CPR", "Cardiopulmonary Resuscitation"),
        ProtocolCategory("heimlich", "Heimlich", "Choking response"),
        ProtocolCategory("bandaging", "Bandaging", "Wound care")
    )

    init {
        loadCategoryStates()
    }

    private fun loadCategoryStates() {
        viewModelScope.launch {
            _uiState.value = CategoryUiState.Loading

            try {
                // Filter protocols based on mode
                val protocols = if (isEmergencyMode) {
                    // Emergency mode: Only CPR and Heimlich
                    allProtocols.filter { it.id in listOf("cpr", "heimlich") }
                } else {
                    // Instructional mode: All protocols available
                    allProtocols
                }

                // Load lock states for each protocol
                val categoryStates = protocols.map { protocol ->
                    val lockState = if (isEmergencyMode) {
                        loadEmergencyLockState(protocol.id)
                    } else {
                        // All unlocked in instructional mode
                        EmergencyLockState(
                            isUnlocked = true,
                            requiredVariants = emptyList(),
                            completedVariants = emptyList(),
                            progress = Pair(0, 0)
                        )
                    }

                    CategoryState(
                        protocol = protocol,
                        lockState = lockState
                    )
                }

                _uiState.value = CategoryUiState.Success(
                    categories = categoryStates,
                    isEmergencyMode = isEmergencyMode
                )

                Timber.d("Loaded ${categoryStates.size} categories for $mode mode")

            } catch (e: Exception) {
                _uiState.value = CategoryUiState.Error(
                    e.message ?: "Failed to load categories"
                )
                Timber.e(e, "Failed to load category states")
            }
        }
    }

    private suspend fun loadEmergencyLockState(protocolId: String): EmergencyLockState {
        val isUnlocked = emergencyUnlockManager.isEmergencyUnlocked(protocolId)
        val requiredVariants = emergencyUnlockManager.getRequiredVariants(protocolId)
        val variantStatus = emergencyUnlockManager.getRequiredVariantsStatus(protocolId)
        val completedVariants = variantStatus.filter { it.value }.keys.toList()
        val progress = emergencyUnlockManager.getUnlockProgress(protocolId)

        return EmergencyLockState(
            isUnlocked = isUnlocked,
            requiredVariants = requiredVariants,
            completedVariants = completedVariants,
            progress = progress
        )
    }

    /**
     * Checks if protocol can be selected.
     * In emergency mode, only unlocked protocols are accessible.
     * Bandaging is never accessible in emergency mode.
     */
    fun canSelectProtocol(protocolId: String): Boolean {
        val state = _uiState.value as? CategoryUiState.Success ?: return false

        // Prevent bandaging selection in emergency mode
        if (isEmergencyMode && protocolId == "bandaging") {
            Timber.w("Attempted to select bandaging in emergency mode - blocked")
            return false
        }

        val categoryState = state.categories.find { it.protocol.id == protocolId }

        return if (isEmergencyMode) {
            categoryState?.lockState?.isUnlocked ?: false
        } else {
            true // All unlocked in instructional
        }
    }

    /**
     * Gets lock state for a protocol.
     */
    fun getLockState(protocolId: String): EmergencyLockState? {
        val state = _uiState.value as? CategoryUiState.Success ?: return null
        return state.categories.find { it.protocol.id == protocolId }?.lockState
    }

    /**
     * Refreshes lock states (call after completing training).
     */
    fun refreshLockStates() {
        loadCategoryStates()
    }
}

/**
 * UI state for category screen.
 */
sealed class CategoryUiState {
    data object Loading : CategoryUiState()

    data class Success(
        val categories: List<CategoryState>,
        val isEmergencyMode: Boolean
    ) : CategoryUiState() {
        val hasAnyUnlocked: Boolean
            get() = categories.any { it.lockState.isUnlocked }

        val allLocked: Boolean
            get() = categories.none { it.lockState.isUnlocked }
    }

    data class Error(val message: String) : CategoryUiState()
}

/**
 * Category with lock state.
 */
data class CategoryState(
    val protocol: ProtocolCategory,
    val lockState: EmergencyLockState
)

/**
 * Protocol category info.
 */
data class ProtocolCategory(
    val id: String,
    val name: String,
    val description: String
)