package com.voxaid.feature.main.variant

import com.voxaid.core.common.model.ProtocolCompletion

/**
 * UI state for protocol variant lock status.
 * Combines variant info with completion/unlock status.
 */
data class ProtocolLockState(
    val variantId: String,
    val name: String,
    val description: String,
    val isUnlocked: Boolean,
    val completion: ProtocolCompletion? = null
) {
    /**
     * User-friendly message explaining lock state.
     */
    val lockMessage: String
        get() = when {
            isUnlocked -> "Unlocked for Emergency Mode"
            completion?.hasStarted == true ->
                "Complete instructional mode to unlock (${completion.progressPercentage}% done)"
            else -> "Complete instructional mode to unlock"
        }

    /**
     * Whether to show progress indicator.
     */
    val showProgress: Boolean
        get() = !isUnlocked && (completion?.hasStarted == true)

    /**
     * Whether variant can be selected in current mode.
     */
    fun canSelect(isEmergencyMode: Boolean): Boolean {
        return if (isEmergencyMode) isUnlocked else true
    }
}

/**
 * UI state for variant selection screen.
 */
sealed class VariantScreenUiState {
    data object Loading : VariantScreenUiState()

    data class Success(
        val variants: List<ProtocolLockState>,
        val isEmergencyMode: Boolean
    ) : VariantScreenUiState() {
        val hasAnyUnlocked: Boolean
            get() = variants.any { it.isUnlocked }

        val allLocked: Boolean
            get() = variants.none { it.isUnlocked }

        val unlockedCount: Int
            get() = variants.count { it.isUnlocked }

        val totalCount: Int
            get() = variants.size
    }

    data class Error(val message: String) : VariantScreenUiState()
}