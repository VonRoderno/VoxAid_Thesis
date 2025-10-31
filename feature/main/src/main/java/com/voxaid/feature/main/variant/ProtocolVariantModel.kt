package com.voxaid.feature.main.variant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voxaid.core.common.datastore.ProtocolCompletionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for protocol variant selection screen.
 * Manages variant list with lock/unlock status based on completion.
 */
@HiltViewModel
class ProtocolVariantViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val completionManager: ProtocolCompletionManager
) : ViewModel() {

    private val protocolId: String = savedStateHandle.get<String>("protocol") ?: "cpr"
    private val mode: String = savedStateHandle.get<String>("mode") ?: "instructional"

    private val isEmergencyMode = mode == "emergency"

    private val _uiState = MutableStateFlow<VariantScreenUiState>(VariantScreenUiState.Loading)
    val uiState: StateFlow<VariantScreenUiState> = _uiState.asStateFlow()

    init {
        loadVariants()
    }

    private fun loadVariants() {
        viewModelScope.launch {
            try {
                val variants = getProtocolVariants(protocolId)

                // Load lock state for each variant
                val lockStates = variants.map { variant ->
                    val isUnlocked = if (isEmergencyMode) {
                        completionManager.isVariantUnlocked(variant.id)
                    } else {
                        true // All variants unlocked in instructional mode
                    }

                    val completion = if (isEmergencyMode) {
                        completionManager.getCompletionData(variant.id, 6) // Estimate 6 steps
                    } else null

                    ProtocolLockState(
                        variantId = variant.id,
                        name = variant.name,
                        description = variant.description,
                        isUnlocked = isUnlocked,
                        completion = completion
                    )
                }

                _uiState.value = VariantScreenUiState.Success(
                    variants = lockStates,
                    isEmergencyMode = isEmergencyMode
                )

                Timber.d("Loaded ${lockStates.size} variants for $protocolId in $mode mode")
                if (isEmergencyMode) {
                    val unlockedCount = lockStates.count { it.isUnlocked }
                    Timber.d("$unlockedCount/${lockStates.size} variants unlocked")
                }

            } catch (e: Exception) {
                _uiState.value = VariantScreenUiState.Error(
                    e.message ?: "Failed to load variants"
                )
                Timber.e(e, "Failed to load variants")
            }
        }
    }

    /**
     * Called when user attempts to select a locked variant.
     * Returns true if selection should be allowed, false otherwise.
     */
    fun canSelectVariant(variantId: String): Boolean {
        val state = _uiState.value as? VariantScreenUiState.Success ?: return false
        val variant = state.variants.find { it.variantId == variantId } ?: return false

        return variant.canSelect(isEmergencyMode)
    }

    /**
     * Gets the lock state for a specific variant.
     */
    fun getVariantLockState(variantId: String): ProtocolLockState? {
        val state = _uiState.value as? VariantScreenUiState.Success ?: return null
        return state.variants.find { it.variantId == variantId }
    }

    /**
     * Helper to get variants for a protocol (same as in ProtocolVariantScreen).
     * TODO: Move to repository layer for better separation of concerns.
     */
    private fun getProtocolVariants(protocolId: String): List<VariantData> {
        return when (protocolId) {
            "cpr" -> listOf(
                VariantData(
                    id = "cpr_1person",
                    name = "1-Person CPR",
                    description = "Standard CPR performed by one rescuer"
                ),
                VariantData(
                    id = "cpr_2person",
                    name = "2-Person CPR",
                    description = "CPR with two rescuers alternating compressions"
                ),
                VariantData(
                    id = "cpr_aed",
                    name = "CPR with AED",
                    description = "CPR combined with Automated External Defibrillator"
                ),
                VariantData(
                    id = "cpr_no_aed",
                    name = "CPR without AED",
                    description = "Standard CPR when AED is not available"
                )
            )

            "heimlich" -> listOf(
                VariantData(
                    id = "heimlich_others",
                    name = "Heimlich for Others",
                    description = "Perform Heimlich maneuver on another person"
                ),
                VariantData(
                    id = "heimlich_self",
                    name = "Self Heimlich",
                    description = "Perform Heimlich maneuver on yourself"
                )
            )

            "bandaging" -> listOf(
                VariantData(
                    id = "bandaging_triangular",
                    name = "Triangular Bandaging",
                    description = "Using triangular bandages for slings and wounds"
                ),
                VariantData(
                    id = "bandaging_circular",
                    name = "Circular Bandaging",
                    description = "Roller bandage technique for limbs and joints"
                )
            )

            else -> emptyList()
        }
    }

    /**
     * Simple data class for variant information.
     */
    private data class VariantData(
        val id: String,
        val name: String,
        val description: String
    )
}