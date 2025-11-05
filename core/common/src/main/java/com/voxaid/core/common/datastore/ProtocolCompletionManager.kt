package com.voxaid.core.common.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.voxaid.core.common.model.CompletionStats
import com.voxaid.core.common.model.ProtocolCompletion
import com.voxaid.core.common.model.UnlockResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// DataStore extension for completion tracking
private val Context.completionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "protocol_completion"
)

/**
 * Manages protocol completion state and unlocking logic.
 *
 * Updated: New variant IDs for bandaging protocols
 *
 * Storage Keys:
 * - "protocol_completed_{variantId}": Boolean (completion status)
 * - "protocol_completed_at_{variantId}": Long (completion timestamp)
 * - "protocol_progress_{variantId}": Int (last reached step)
 */
@Singleton
class ProtocolCompletionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.completionDataStore

    companion object {
        private const val KEY_PREFIX_COMPLETED = "protocol_completed_"
        private const val KEY_PREFIX_COMPLETED_AT = "protocol_completed_at_"
        private const val KEY_PREFIX_PROGRESS = "protocol_progress_"

        /**
         * All available protocol variants in the app.
         * Updated with new bandaging variants.
         */
        private val ALL_VARIANTS = listOf(
            // CPR variants
            "cpr_1person",
            "cpr_2person",
            "cpr_aed",

            // Heimlich variants
            "heimlich_others",
            "heimlich_self",

            // Bandaging variants (updated)
            "bandaging_head",
            "bandaging_hand",
            "bandaging_arm_sling"
        )
    }

    /**
     * Checks if a specific variant is unlocked for emergency mode.
     */
    suspend fun isVariantUnlocked(variantId: String): Boolean {
        val key = booleanPreferencesKey("$KEY_PREFIX_COMPLETED$variantId")
        return dataStore.data.map { preferences ->
            preferences[key] ?: false
        }.first()
    }

    /**
     * Flow that emits unlock status for a specific variant.
     */
    fun isVariantUnlockedFlow(variantId: String): Flow<Boolean> {
        val key = booleanPreferencesKey("$KEY_PREFIX_COMPLETED$variantId")
        return dataStore.data.map { preferences ->
            preferences[key] ?: false
        }
    }

    /**
     * Gets full completion data for a variant.
     */
    suspend fun getCompletionData(variantId: String, totalSteps: Int): ProtocolCompletion {
        return dataStore.data.map { preferences ->
            val completedKey = booleanPreferencesKey("$KEY_PREFIX_COMPLETED$variantId")
            val timestampKey = longPreferencesKey("$KEY_PREFIX_COMPLETED_AT$variantId")
            val progressKey = intPreferencesKey("$KEY_PREFIX_PROGRESS$variantId")

            ProtocolCompletion(
                variantId = variantId,
                isCompleted = preferences[completedKey] ?: false,
                completedAt = preferences[timestampKey],
                lastStepReached = preferences[progressKey] ?: 0,
                totalSteps = totalSteps
            )
        }.first()
    }

    /**
     * Flow of completion data for a variant.
     */
    fun getCompletionDataFlow(variantId: String, totalSteps: Int): Flow<ProtocolCompletion> {
        return dataStore.data.map { preferences ->
            val completedKey = booleanPreferencesKey("$KEY_PREFIX_COMPLETED$variantId")
            val timestampKey = longPreferencesKey("$KEY_PREFIX_COMPLETED_AT$variantId")
            val progressKey = intPreferencesKey("$KEY_PREFIX_PROGRESS$variantId")

            ProtocolCompletion(
                variantId = variantId,
                isCompleted = preferences[completedKey] ?: false,
                completedAt = preferences[timestampKey],
                lastStepReached = preferences[progressKey] ?: 0,
                totalSteps = totalSteps
            )
        }
    }

    /**
     * Updates progress for a variant (called when user navigates to a step).
     */
    suspend fun updateProgress(variantId: String, stepIndex: Int) {
        dataStore.edit { preferences ->
            val progressKey = intPreferencesKey("$KEY_PREFIX_PROGRESS$variantId")
            val currentProgress = preferences[progressKey] ?: 0

            // Only update if new step is further than previously reached
            if (stepIndex > currentProgress) {
                preferences[progressKey] = stepIndex
                Timber.d("Updated progress for $variantId: step $stepIndex")
            }
        }
    }

    /**
     * Marks a variant as completed (unlocks it for emergency mode).
     * Called when user reaches the final step in instructional mode.
     */
    suspend fun markAsCompleted(variantId: String): UnlockResult {
        val wasUnlocked = isVariantUnlocked(variantId)

        if (wasUnlocked) {
            Timber.d("Variant $variantId already unlocked")
            return UnlockResult.AlreadyUnlocked
        }

        dataStore.edit { preferences ->
            val completedKey = booleanPreferencesKey("$KEY_PREFIX_COMPLETED$variantId")
            val timestampKey = longPreferencesKey("$KEY_PREFIX_COMPLETED_AT$variantId")

            preferences[completedKey] = true
            preferences[timestampKey] = System.currentTimeMillis()
        }

        Timber.i("Variant $variantId unlocked for emergency mode")
        return UnlockResult.NewlyUnlocked(variantId)
    }

    /**
     * Resets completion status for a variant (for testing/debugging).
     */
    suspend fun resetVariant(variantId: String) {
        dataStore.edit { preferences ->
            val completedKey = booleanPreferencesKey("$KEY_PREFIX_COMPLETED$variantId")
            val timestampKey = longPreferencesKey("$KEY_PREFIX_COMPLETED_AT$variantId")
            val progressKey = intPreferencesKey("$KEY_PREFIX_PROGRESS$variantId")

            preferences.remove(completedKey)
            preferences.remove(timestampKey)
            preferences.remove(progressKey)
        }

        Timber.d("Reset completion data for $variantId")
    }

    /**
     * Resets all completion data (for testing/debugging).
     */
    suspend fun resetAll() {
        dataStore.edit { preferences ->
            ALL_VARIANTS.forEach { variantId ->
                val completedKey = booleanPreferencesKey("$KEY_PREFIX_COMPLETED$variantId")
                val timestampKey = longPreferencesKey("$KEY_PREFIX_COMPLETED_AT$variantId")
                val progressKey = intPreferencesKey("$KEY_PREFIX_PROGRESS$variantId")

                preferences.remove(completedKey)
                preferences.remove(timestampKey)
                preferences.remove(progressKey)
            }
        }

        Timber.w("Reset all completion data")
    }

    /**
     * Gets completion statistics across all variants.
     */
    suspend fun getCompletionStats(): CompletionStats {
        return dataStore.data.map { preferences ->
            var completed = 0
            var inProgress = 0
            var notStarted = 0

            ALL_VARIANTS.forEach { variantId ->
                val completedKey = booleanPreferencesKey("$KEY_PREFIX_COMPLETED$variantId")
                val progressKey = intPreferencesKey("$KEY_PREFIX_PROGRESS$variantId")

                val isCompleted = preferences[completedKey] ?: false
                val progress = preferences[progressKey] ?: 0

                when {
                    isCompleted -> completed++
                    progress > 0 -> inProgress++
                    else -> notStarted++
                }
            }

            CompletionStats(
                totalVariants = ALL_VARIANTS.size,
                completedVariants = completed,
                inProgressVariants = inProgress,
                notStartedVariants = notStarted
            )
        }.first()
    }

    /**
     * Gets list of all unlocked variant IDs.
     */
    suspend fun getUnlockedVariants(): List<String> {
        return dataStore.data.map { preferences ->
            ALL_VARIANTS.filter { variantId ->
                val key = booleanPreferencesKey("$KEY_PREFIX_COMPLETED$variantId")
                preferences[key] ?: false
            }
        }.first()
    }

    /**
     * Checks if user has completed at least one protocol.
     */
    suspend fun hasCompletedAnyProtocol(): Boolean {
        return getUnlockedVariants().isNotEmpty()
    }

    /**
     * Checks if ALL variants of a protocol are completed.
     * Required for unlocking Emergency Mode.
     *
     * @param protocolId The base protocol (e.g., "cpr", "heimlich", "bandaging")
     * @return true if all variants completed, false otherwise
     */
    suspend fun isProtocolFullyCompleted(protocolId: String): Boolean {
        val variants = getProtocolVariants(protocolId)

        return variants.all { variantId ->
            isVariantUnlocked(variantId)
        }
    }

    /**
     * Gets completion status for entire protocol.
     *
     * @param protocolId The base protocol
     * @return ProtocolCompletionStatus with progress info
     */
    suspend fun getProtocolCompletionStatus(protocolId: String): ProtocolCompletionStatus {
        val variants = getProtocolVariants(protocolId)

        val completedVariants = variants.count { variantId ->
            isVariantUnlocked(variantId)
        }

        return ProtocolCompletionStatus(
            protocolId = protocolId,
            totalVariants = variants.size,
            completedVariants = completedVariants,
            isFullyCompleted = completedVariants == variants.size,
            completedVariantIds = variants.filter { isVariantUnlocked(it) }
        )
    }

    /**
     * Get all variants for a protocol.
     * Helper method for protocol-level checking.
     */
    private fun getProtocolVariants(protocolId: String): List<String> {
        return when (protocolId) {
            "cpr" -> listOf(
                "cpr_1person",
                "cpr_2person",
                "cpr_aed"
            )
            "heimlich" -> listOf(
                "heimlich_others",
                "heimlich_self"
            )
            "bandaging" -> listOf(
                "bandaging_head",
                "bandaging_hand",
                "bandaging_arm_sling"
            )
            else -> emptyList()
        }
    }

    /**
     * Get protocol display names for UI.
     */
    fun getProtocolDisplayName(protocolId: String): String {
        return when (protocolId) {
            "cpr" -> "CPR (Cardiopulmonary Resuscitation)"
            "heimlich" -> "Heimlich Maneuver"
            "bandaging" -> "Wound Bandaging"
            else -> protocolId
        }
    }

    data class ProtocolCompletionStatus(
        val protocolId: String,
        val totalVariants: Int,
        val completedVariants: Int,
        val isFullyCompleted: Boolean,
        val completedVariantIds: List<String>
    ) {
        /**
         * Progress percentage (0-100).
         */
        val progressPercentage: Int
            get() = if (totalVariants > 0) {
                ((completedVariants.toFloat() / totalVariants) * 100).toInt()
            } else 0

        /**
         * User-friendly status message.
         */
        val statusMessage: String
            get() = when {
                isFullyCompleted -> "Unlocked for Emergency Mode"
                completedVariants > 0 -> "$completedVariants of $totalVariants variants completed"
                else -> "Complete all variants to unlock Emergency Mode"
            }
    }
}