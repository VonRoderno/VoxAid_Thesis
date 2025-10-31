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
         * Used for calculating completion statistics.
         */
        private val ALL_VARIANTS = listOf(
            "cpr_1person",
            "cpr_2person",
            "cpr_aed",
            "cpr_no_aed",
            "heimlich_others",
            "heimlich_self",
            "bandaging_triangular",
            "bandaging_circular"
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
}