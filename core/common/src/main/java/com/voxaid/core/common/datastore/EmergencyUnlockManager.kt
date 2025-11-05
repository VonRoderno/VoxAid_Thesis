package com.voxaid.core.common.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val Context.emergencyDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "emergency_unlock"
)

/**
 * Manages emergency mode unlock status.
 *
 * Emergency protocols are unlocked only after completing ALL required learning variants.
 * Example: emergency_cpr requires completing cpr_learning
 * Example: emergency_heimlich requires completing heimlich_self AND heimlich_others
 *
 * Storage Keys:
 * - "emergency_unlocked_{protocolId}": Boolean (unlock status)
 */
@Singleton
class EmergencyUnlockManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val completionManager: ProtocolCompletionManager
) {
    private val dataStore = context.emergencyDataStore

    companion object {
        private const val KEY_PREFIX_EMERGENCY_UNLOCKED = "emergency_unlocked_"

        /**
         * Maps emergency protocol IDs to their required learning variants.
         * Must complete ALL listed variants to unlock emergency mode.
         */
        private val EMERGENCY_REQUIREMENTS = mapOf(
            "emergency_cpr" to listOf("cpr_learning"),
            "emergency_heimlich" to listOf("heimlich_self", "heimlich_others"),
            "emergency_bandaging" to listOf("bandaging_head", "bandaging_hand", "bandaging_arm_sling")
        )

        /**
         * Protocol categories that have emergency mode.
         */
        private val EMERGENCY_PROTOCOLS = mapOf(
            "cpr" to "emergency_cpr",
            "heimlich" to "emergency_heimlich",
            "bandaging" to "emergency_bandaging"
        )
    }

    /**
     * Checks if emergency mode is unlocked for a protocol category.
     *
     * @param protocolCategory Base protocol ID (e.g., "cpr", "heimlich")
     * @return true if all required learning variants are completed
     */
    suspend fun isEmergencyUnlocked(protocolCategory: String): Boolean {
        val emergencyId = EMERGENCY_PROTOCOLS[protocolCategory] ?: return false
        val requiredVariants = EMERGENCY_REQUIREMENTS[emergencyId] ?: return false

        // Check if all required variants are completed
        val allCompleted = requiredVariants.all { variantId ->
            completionManager.isVariantUnlocked(variantId)
        }

        // Cache the result
        if (allCompleted) {
            cacheUnlockStatus(emergencyId, true)
        }

        return allCompleted
    }

    /**
     * Flow that emits unlock status for emergency mode.
     */
    fun isEmergencyUnlockedFlow(protocolCategory: String): Flow<Boolean> {
        val emergencyId = EMERGENCY_PROTOCOLS[protocolCategory] ?: return flowOf(false)

        return dataStore.data.map { preferences ->
            val key = booleanPreferencesKey("$KEY_PREFIX_EMERGENCY_UNLOCKED$emergencyId")
            preferences[key] ?: false
        }
    }

    /**
     * Gets list of required learning variants for emergency mode.
     */
    fun getRequiredVariants(protocolCategory: String): List<String> {
        val emergencyId = EMERGENCY_PROTOCOLS[protocolCategory] ?: return emptyList()
        return EMERGENCY_REQUIREMENTS[emergencyId] ?: emptyList()
    }

    /**
     * Gets completion status of required variants.
     * Returns map of variantId -> isCompleted
     */
    suspend fun getRequiredVariantsStatus(protocolCategory: String): Map<String, Boolean> {
        val requiredVariants = getRequiredVariants(protocolCategory)

        return requiredVariants.associateWith { variantId ->
            completionManager.isVariantUnlocked(variantId)
        }
    }

    /**
     * Gets progress toward unlocking emergency mode.
     * Returns Pair<completed, total>
     */
    suspend fun getUnlockProgress(protocolCategory: String): Pair<Int, Int> {
        val status = getRequiredVariantsStatus(protocolCategory)
        val completed = status.values.count { it }
        val total = status.size

        return Pair(completed, total)
    }

    /**
     * Checks if emergency mode just became unlocked (for showing celebration).
     */
    suspend fun checkAndMarkNewlyUnlocked(protocolCategory: String): Boolean {
        val wasUnlocked = isEmergencyUnlockedCached(protocolCategory)
        val isNowUnlocked = isEmergencyUnlocked(protocolCategory)

        return !wasUnlocked && isNowUnlocked
    }

    /**
     * Gets emergency protocol ID for a category.
     */
    fun getEmergencyProtocolId(protocolCategory: String): String? {
        return EMERGENCY_PROTOCOLS[protocolCategory]
    }

    /**
     * Checks all protocols and returns list of newly unlocked emergency modes.
     */
    suspend fun checkAllForNewUnlocks(): List<String> {
        val newlyUnlocked = mutableListOf<String>()

        EMERGENCY_PROTOCOLS.keys.forEach { category ->
            if (checkAndMarkNewlyUnlocked(category)) {
                newlyUnlocked.add(category)
            }
        }

        return newlyUnlocked
    }

    /**
     * Resets emergency unlock status (for testing).
     */
    suspend fun resetEmergencyUnlock(protocolCategory: String) {
        val emergencyId = EMERGENCY_PROTOCOLS[protocolCategory] ?: return

        dataStore.edit { preferences ->
            val key = booleanPreferencesKey("$KEY_PREFIX_EMERGENCY_UNLOCKED$emergencyId")
            preferences.remove(key)
        }

        Timber.d("Reset emergency unlock for $protocolCategory")
    }

    /**
     * Resets all emergency unlocks (for testing).
     */
    suspend fun resetAllEmergencyUnlocks() {
        dataStore.edit { preferences ->
            EMERGENCY_PROTOCOLS.values.forEach { emergencyId ->
                val key = booleanPreferencesKey("$KEY_PREFIX_EMERGENCY_UNLOCKED$emergencyId")
                preferences.remove(key)
            }
        }

        Timber.w("Reset all emergency unlocks")
    }

    /**
     * Private helper to cache unlock status.
     */
    private suspend fun cacheUnlockStatus(emergencyId: String, isUnlocked: Boolean) {
        dataStore.edit { preferences ->
            val key = booleanPreferencesKey("$KEY_PREFIX_EMERGENCY_UNLOCKED$emergencyId")
            preferences[key] = isUnlocked
        }
    }

    /**
     * Private helper to check cached unlock status.
     */
    private suspend fun isEmergencyUnlockedCached(protocolCategory: String): Boolean {
        val emergencyId = EMERGENCY_PROTOCOLS[protocolCategory] ?: return false

        return dataStore.data.map { preferences ->
            val key = booleanPreferencesKey("$KEY_PREFIX_EMERGENCY_UNLOCKED$emergencyId")
            preferences[key] ?: false
        }.first()
    }
}

/**
 * UI state for emergency unlock status.
 */
data class EmergencyLockState(
    val isUnlocked: Boolean,
    val requiredVariants: List<String>,
    val completedVariants: List<String>,
    val progress: Pair<Int, Int> // completed / total
) {
    val progressPercentage: Int
        get() = if (progress.second > 0) {
            ((progress.first.toFloat() / progress.second) * 100).toInt()
        } else 0

    val lockMessage: String
        get() = if (isUnlocked) {
            "✓ Emergency Mode Unlocked"
        } else {
            "🔒 Complete ${progress.second - progress.first} more training${if (progress.second - progress.first > 1) "s" else ""} to unlock"
        }

    val detailedMessage: String
        get() = if (isUnlocked) {
            "All training completed. Emergency mode is ready."
        } else {
            "Progress: ${progress.first}/${progress.second} required trainings completed"
        }
}