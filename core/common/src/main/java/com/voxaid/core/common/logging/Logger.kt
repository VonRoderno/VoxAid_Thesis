package com.voxaid.core.common.logging

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Structured logger for VoxAid.
 * Provides consistent logging across the app with context.
 */
@Singleton
class Logger @Inject constructor(
    private val crashReporter: CrashReporter
) {

    /**
     * Logs user action for analytics.
     */
    fun logUserAction(action: String, params: Map<String, Any> = emptyMap()) {
        val message = buildString {
            append("User Action: $action")
            if (params.isNotEmpty()) {
                append(" | ")
                append(params.entries.joinToString(", ") { "${it.key}=${it.value}" })
            }
        }
        Timber.i(message)
        crashReporter.logMessage(message, android.util.Log.INFO)
    }

    /**
     * Logs protocol navigation.
     */
    fun logProtocolNavigation(protocolId: String, stepNumber: Int, mode: String) {
        logUserAction("protocol_navigation", mapOf(
            "protocol" to protocolId,
            "step" to stepNumber,
            "mode" to mode
        ))
    }

    /**
     * Logs voice command recognition.
     */
    fun logVoiceCommand(command: String, intent: String) {
        logUserAction("voice_command", mapOf(
            "command" to command,
            "intent" to intent
        ))
    }

    /**
     * Logs feature usage.
     */
    fun logFeatureUsage(feature: String, enabled: Boolean) {
        logUserAction("feature_${if (enabled) "enabled" else "disabled"}", mapOf(
            "feature" to feature
        ))
    }

    /**
     * Logs error with context.
     */
    fun logError(error: Throwable, context: String, additionalInfo: Map<String, Any> = emptyMap()) {
        val message = buildString {
            append("Error in $context")
            if (additionalInfo.isNotEmpty()) {
                append(" | ")
                append(additionalInfo.entries.joinToString(", ") { "${it.key}=${it.value}" })
            }
        }

        Timber.e(error, message)
        crashReporter.logException(error, message)
    }

    /**
     * Logs performance metric.
     */
    fun logPerformance(metric: String, durationMs: Long) {
        val message = "Performance: $metric took ${durationMs}ms"
        Timber.d(message)

        if (durationMs > 1000) {
            // Log slow operations
            crashReporter.logMessage(message, android.util.Log.WARN)
        }
    }
}