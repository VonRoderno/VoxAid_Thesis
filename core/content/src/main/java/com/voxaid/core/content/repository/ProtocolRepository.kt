package com.voxaid.core.content.repository

import android.content.Context
import com.google.gson.Gson
import com.voxaid.core.content.model.Protocol
import com.voxaid.core.content.model.isValid
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for loading protocol data from JSON assets.
 * Provides cached access to protocol definitions.
 */
@Singleton
class ProtocolRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    private val protocolCache = mutableMapOf<String, Protocol>()

    /**
     * Loads a protocol by ID from assets.
     * Results are cached for performance.
     */
    suspend fun getProtocol(protocolId: String): Result<Protocol> = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            protocolCache[protocolId]?.let {
                Timber.d("Loading protocol $protocolId from cache")
                return@withContext Result.success(it)
            }

            // Load from assets
            val jsonFileName = "protocols/$protocolId.json"
            Timber.d("Loading protocol from assets: $jsonFileName")

            val jsonString = context.assets.open(jsonFileName).bufferedReader().use { it.readText() }
            val protocol = gson.fromJson(jsonString, Protocol::class.java)

            // Validate protocol
            if (!protocol.isValid()) {
                Timber.e("Invalid protocol data for $protocolId")
                return@withContext Result.failure(
                    IllegalStateException("Invalid protocol data for $protocolId")
                )
            }

            // Cache and return
            protocolCache[protocolId] = protocol
            Timber.i("Successfully loaded protocol: ${protocol.name}")
            Result.success(protocol)

        } catch (e: IOException) {
            Timber.e(e, "Failed to load protocol $protocolId")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Error parsing protocol $protocolId")
            Result.failure(e)
        }
    }

    /**
     * Preloads all available protocols into cache.
     * Call this during app initialization for better UX.
     */
    suspend fun preloadProtocols() = withContext(Dispatchers.IO) {
        val protocolIds = listOf("cpr", "heimlich", "bandaging")

        protocolIds.forEach { id ->
            if (!protocolCache.containsKey(id)) {
                getProtocol(id)
            }
        }

        Timber.i("Preloaded ${protocolCache.size} protocols")
    }

    /**
     * Clears the protocol cache.
     * Useful for testing or forcing reload.
     */
    fun clearCache() {
        protocolCache.clear()
        Timber.d("Protocol cache cleared")
    }
}