package com.voxaid.core.common.model

/**
 * Data model for protocol completion status.
 * Tracks which protocols have been completed in instructional mode.
 */
data class ProtocolCompletion(
    val variantId: String,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null, // Timestamp in milliseconds
    val lastStepReached: Int = 0,
    val totalSteps: Int = 0
) {
    /**
     * Progress percentage (0-100).
     */
    val progressPercentage: Int
        get() = if (totalSteps > 0) {
            ((lastStepReached.toFloat() / totalSteps) * 100).toInt()
        } else 0

    /**
     * Whether user has made any progress.
     */
    val hasStarted: Boolean
        get() = lastStepReached > 0

    /**
     * Whether this is the final step.
     */
    fun isFinalStep(currentStep: Int): Boolean {
        return currentStep >= totalSteps - 1
    }
}

/**
 * Result wrapper for protocol unlock operations.
 */
sealed class UnlockResult {
    data object AlreadyUnlocked : UnlockResult()
    data class NewlyUnlocked(val variantId: String) : UnlockResult()
    data class StillLocked(val variantId: String, val reason: String) : UnlockResult()
}

/**
 * Statistics for protocol completion across all variants.
 */
data class CompletionStats(
    val totalVariants: Int,
    val completedVariants: Int,
    val inProgressVariants: Int,
    val notStartedVariants: Int
) {
    val completionRate: Float
        get() = if (totalVariants > 0) {
            completedVariants.toFloat() / totalVariants
        } else 0f

    val completionPercentage: Int
        get() = (completionRate * 100).toInt()
}