package com.voxaid.core.common.error

/**
 * Sealed class representing all possible errors in VoxAid.
 * Provides user-friendly messages and recovery actions.
 */
sealed class VoxError(
    open val message: String,
    open val cause: Throwable? = null
) {

    /**
     * Network-related errors.
     */
    data class NetworkError(
        override val message: String = "Network connection failed",
        override val cause: Throwable? = null
    ) : VoxError(message, cause)

    /**
     * Update check failed.
     */
    data class UpdateCheckError(
        override val message: String = "Unable to check for updates",
        override val cause: Throwable? = null
    ) : VoxError(message, cause)

    /**
     * Protocol loading errors.
     */
    data class ProtocolLoadError(
        val protocolId: String,
        override val message: String = "Failed to load protocol: $protocolId",
        override val cause: Throwable? = null
    ) : VoxError(message, cause)

    /**
     * ASR initialization errors.
     */
    data class AsrError(
        override val message: String = "Voice recognition unavailable",
        override val cause: Throwable? = null
    ) : VoxError(message, cause)

    /**
     * TTS errors.
     */
    data class TtsError(
        override val message: String = "Text-to-speech unavailable",
        override val cause: Throwable? = null
    ) : VoxError(message, cause)

    /**
     * Permission errors.
     */
    data class PermissionError(
        val permission: String,
        override val message: String = "Permission required: $permission",
        override val cause: Throwable? = null
    ) : VoxError(message, cause)

    /**
     * Unknown/unexpected errors.
     */
    data class UnknownError(
        override val message: String = "An unexpected error occurred",
        override val cause: Throwable? = null
    ) : VoxError(message, cause)
}

/**
 * User-friendly error messages.
 */
fun VoxError.toUserMessage(): String = when (this) {
    is VoxError.NetworkError -> "Unable to connect. Please check your internet connection and try again."
    is VoxError.UpdateCheckError -> "Couldn't check for updates. You can continue using the app."
    is VoxError.ProtocolLoadError -> "Failed to load instructions. Please restart the app."
    is VoxError.AsrError -> "Voice recognition is unavailable. Use manual navigation instead."
    is VoxError.TtsError -> "Voice guidance is unavailable. Instructions will be shown on screen."
    is VoxError.PermissionError -> "This feature requires permission: ${this.permission}"
    is VoxError.UnknownError -> "Something went wrong. Please try again."
}

/**
 * Suggested user actions.
 */
fun VoxError.toRecoveryAction(): String? = when (this) {
    is VoxError.NetworkError -> "Retry"
    is VoxError.UpdateCheckError -> null // Can continue without update
    is VoxError.ProtocolLoadError -> "Restart App"
    is VoxError.AsrError -> null // Can use manual controls
    is VoxError.TtsError -> null // Can read on screen
    is VoxError.PermissionError -> "Grant Permission"
    is VoxError.UnknownError -> "Retry"
}