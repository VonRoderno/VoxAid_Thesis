package com.voxaid.core.content.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.voxaid.core.content.model.EmergencyProtocol
import com.voxaid.core.content.model.EmergencyStepData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for loading emergency mode protocols.
 * Loads from JSON assets with step engine support.
 */
@Singleton
class EmergencyProtocolRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val protocolCache = mutableMapOf<String, EmergencyProtocol>()

    /**
     * Loads an emergency protocol by ID.
     */
    suspend fun getEmergencyProtocol(protocolId: String): Result<EmergencyProtocol> =
        withContext(Dispatchers.IO) {
            try {
                // Check cache first
                protocolCache[protocolId]?.let {
                    Timber.d("Loading emergency protocol $protocolId from cache")
                    return@withContext Result.success(it)
                }

                // Load from assets
                val jsonFileName = "protocols/$protocolId.json"
                Timber.d("Loading emergency protocol from assets: $jsonFileName")

                val jsonString = context.assets.open(jsonFileName)
                    .bufferedReader()
                    .use { it.readText() }

                // Parse with custom type handling
                val protocol = parseEmergencyProtocol(jsonString)

                // Validate
                if (protocol.steps.isEmpty()) {
                    Timber.e("Emergency protocol $protocolId has no steps")
                    return@withContext Result.failure(
                        IllegalStateException("Emergency protocol has no steps")
                    )
                }

                // Cache and return
                protocolCache[protocolId] = protocol
                Timber.i("Successfully loaded emergency protocol: ${protocol.name}")
                Result.success(protocol)

            } catch (e: IOException) {
                Timber.e(e, "Failed to load emergency protocol $protocolId")
                Result.failure(e)
            } catch (e: Exception) {
                Timber.e(e, "Error parsing emergency protocol $protocolId")
                Result.failure(e)
            }
        }

    /**
     * Parses emergency protocol JSON.
     */
    private fun parseEmergencyProtocol(jsonString: String): EmergencyProtocol {
        // Parse as map to handle dynamic structure
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val json: Map<String, Any> = gson.fromJson(jsonString, type)

        // Safely extract required fields with error logging
        val protocolId = json["id"] as? String
            ?: throw IllegalStateException("Protocol JSON missing required 'id' field")
        val name = json["name"] as? String
            ?: throw IllegalStateException("Protocol $protocolId missing required 'name' field")
        val description = json["description"] as? String ?: ""
        val category = json["category"] as? String
            ?: throw IllegalStateException("Protocol $protocolId missing required 'category' field")
        val initialStepId = json["initial_step_id"] as? String
            ?: throw IllegalStateException("Protocol $protocolId missing required 'initial_step_id' field")
        val warning = json["warning"] as? String
        val metronomeBpm = (json["metronome_bpm"] as? Double)?.toInt()
        val requiredLearningVariants = (json["required_learning_variants"] as? List<String>) ?: emptyList()
        val emergencyNotes = (json["emergency_notes"] as? List<String>) ?: emptyList()

        // Parse steps safely
        @Suppress("UNCHECKED_CAST")
        val stepsMap = json["steps"] as? Map<String, Map<String, Any>>
            ?: throw IllegalStateException("Protocol $protocolId missing or invalid 'steps' object")

        val parsedSteps = stepsMap.mapValues { (stepId, stepJson) ->
            try {
                gson.fromJson(gson.toJson(stepJson), EmergencyStepData::class.java)
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse step '$stepId' in protocol $protocolId")
                throw IllegalStateException("Failed to parse step '$stepId' in protocol $protocolId")
            }
        }

        return EmergencyProtocol(
            id = protocolId,
            name = name,
            description = description,
            category = category,
            requiredLearningVariants = requiredLearningVariants,
            warning = warning,
            emergencyNotes = emergencyNotes,
            metronomeBpm = metronomeBpm,
            initialStepId = initialStepId,
            steps = parsedSteps
        )
    }

    /**
     * Preloads all emergency protocols.
     */
    suspend fun preloadEmergencyProtocols() = withContext(Dispatchers.IO) {
        val emergencyIds = listOf("emergency_cpr", "emergency_heimlich", "emergency_bandaging")

        emergencyIds.forEach { id ->
            if (!protocolCache.containsKey(id)) {
                getEmergencyProtocol(id)
            }
        }

        Timber.i("Preloaded ${protocolCache.size} emergency protocols")
    }

    /**
     * Clears cache.
     */
    fun clearCache() {
        protocolCache.clear()
        Timber.d("Emergency protocol cache cleared")
    }
}