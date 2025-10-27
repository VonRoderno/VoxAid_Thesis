package com.voxaid.core.audio.model

/**
 * State of the audio session and ASR.
 */
data class AudioState(
    val isRecording: Boolean = false,
    val isListening: Boolean = false,
    val micPermissionGranted: Boolean = false,
    val asrReady: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Audio session configuration.
 */
data class AudioConfig(
    val sampleRate: Int = 16000, // 16kHz for Vosk
    val channelConfig: Int = android.media.AudioFormat.CHANNEL_IN_MONO,
    val audioFormat: Int = android.media.AudioFormat.ENCODING_PCM_16BIT,
    val enableAec: Boolean = true, // Acoustic Echo Cancellation
    val enableNs: Boolean = true,  // Noise Suppression
    val enableAgc: Boolean = true  // Automatic Gain Control
)