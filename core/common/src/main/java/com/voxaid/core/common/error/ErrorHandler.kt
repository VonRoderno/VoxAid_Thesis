package com.voxaid.core.common.error

import com.voxaid.core.common.logging.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized error handler for VoxAid.
 * Converts exceptions to VoxError and manages error state.
 */
@Singleton
class ErrorHandler @Inject constructor(
    private val logger: Logger
) {

    private val _currentError = MutableStateFlow<VoxError?>(null)
    val currentError: StateFlow<VoxError?> = _currentError.asStateFlow()

    /**
     * Handles an exception and converts it to VoxError.
     */
    fun handleError(throwable: Throwable, context: String): VoxError {
        val error = mapToVoxError(throwable, context)

        logger.logError(throwable, context, mapOf(
            "error_type" to error::class.simpleName.orEmpty()
        ))

        _currentError.value = error

        return error
    }

    /**
     * Clears the current error.
     */
    fun clearError() {
        _currentError.value = null
    }

    /**
     * Maps throwable to VoxError.
     */
    private fun mapToVoxError(throwable: Throwable, context: String): VoxError {
        return when (throwable) {
            is UnknownHostException, is SocketTimeoutException -> {
                VoxError.NetworkError(cause = throwable)
            }
            is IOException -> {
                when {
                    context.contains("protocol", ignoreCase = true) -> {
                        VoxError.ProtocolLoadError(
                            protocolId = extractProtocolId(context),
                            cause = throwable
                        )
                    }
                    context.contains("update", ignoreCase = true) -> {
                        VoxError.UpdateCheckError(cause = throwable)
                    }
                    else -> VoxError.NetworkError(cause = throwable)
                }
            }
            is SecurityException -> {
                VoxError.PermissionError(
                    permission = extractPermission(throwable.message),
                    cause = throwable
                )
            }
            else -> {
                VoxError.UnknownError(
                    message = throwable.message ?: "An unexpected error occurred",
                    cause = throwable
                )
            }
        }
    }

    private fun extractProtocolId(context: String): String {
        // Try to extract protocol ID from context
        val regex = Regex("protocol[:\\s]*(\\w+)", RegexOption.IGNORE_CASE)
        return regex.find(context)?.groupValues?.getOrNull(1) ?: "unknown"
    }

    private fun extractPermission(message: String?): String {
        message ?: return "unknown"

        return when {
            message.contains("RECORD_AUDIO", ignoreCase = true) -> "Microphone"
            message.contains("CAMERA", ignoreCase = true) -> "Camera"
            message.contains("LOCATION", ignoreCase = true) -> "Location"
            else -> "Unknown"
        }
    }
}

/**
 * Extension function for safe execution with error handling.
 */
suspend fun <T> ErrorHandler.safely(
    context: String,
    block: suspend () -> T
): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        val error = handleError(e, context)
        Result.failure(Exception(error.toUserMessage(), e))
    }
}