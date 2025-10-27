package com.voxaid.core.audio

import android.content.Context
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import com.voxaid.core.audio.model.AudioConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebRTC audio processing implementation.
 * Provides noise suppression, echo cancellation, and automatic gain control.
 */
@Singleton
class WebRtcAudioProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var initialized = false

    // AudioEffect instances
    private var aec: AcousticEchoCanceler? = null
    private var ns: NoiseSuppressor? = null
    private var agc: AutomaticGainControl? = null

    private var currentAudioSessionId: Int = -1

    /**
     * Initializes audio processing with given configuration.
     * @param audioSessionId The AudioRecord session ID to attach effects to
     */
    fun initialize(audioSessionId: Int, config: AudioConfig): Result<Unit> {
        return try {
            if (initialized && currentAudioSessionId == audioSessionId) {
                Timber.d("WebRTC audio processor already initialized for session $audioSessionId")
                return Result.success(Unit)
            }

            // Release any existing effects
            release()

            currentAudioSessionId = audioSessionId

            // Initialize Acoustic Echo Cancellation
            if (config.enableAec) {
                if (AcousticEchoCanceler.isAvailable()) {
                    aec = AcousticEchoCanceler.create(audioSessionId)
                    aec?.enabled = true
                    Timber.i("AEC enabled for session $audioSessionId")
                } else {
                    Timber.w("AEC not available on this device")
                }
            }

            // Initialize Noise Suppression
            if (config.enableNs) {
                if (NoiseSuppressor.isAvailable()) {
                    ns = NoiseSuppressor.create(audioSessionId)
                    ns?.enabled = true
                    Timber.i("Noise Suppressor enabled for session $audioSessionId")
                } else {
                    Timber.w("Noise Suppressor not available on this device")
                }
            }

            // Initialize Automatic Gain Control
            if (config.enableAgc) {
                if (AutomaticGainControl.isAvailable()) {
                    agc = AutomaticGainControl.create(audioSessionId)
                    agc?.enabled = true
                    Timber.i("AGC enabled for session $audioSessionId")
                } else {
                    Timber.w("AGC not available on this device")
                }
            }

            initialized = true
            Timber.i("WebRTC audio processor initialized successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize WebRTC audio processor")
            Result.failure(e)
        }
    }

    /**
     * Processes an audio buffer.
     * Note: With AudioEffect API, processing is automatic.
     * This method is for additional custom processing if needed.
     */
    fun processBuffer(buffer: ShortArray): ShortArray {
        // WebRTC effects process automatically when attached to AudioRecord
        // This is for additional custom processing if needed
        return buffer
    }

    /**
     * Checks if a specific effect is available on the device.
     */
    fun isEffectAvailable(effect: AudioEffect): Boolean {
        return when (effect) {
            AudioEffect.AEC -> AcousticEchoCanceler.isAvailable()
            AudioEffect.NS -> NoiseSuppressor.isAvailable()
            AudioEffect.AGC -> AutomaticGainControl.isAvailable()
        }
    }

    /**
     * Gets status of all effects.
     */
    fun getEffectStatus(): Map<String, Boolean> {
        return mapOf(
            "aec_available" to AcousticEchoCanceler.isAvailable(),
            "aec_enabled" to (aec?.enabled == true),
            "ns_available" to NoiseSuppressor.isAvailable(),
            "ns_enabled" to (ns?.enabled == true),
            "agc_available" to AutomaticGainControl.isAvailable(),
            "agc_enabled" to (agc?.enabled == true)
        )
    }

    /**
     * Releases audio processing resources.
     */
    fun release() {
        try {
            aec?.release()
            aec = null

            ns?.release()
            ns = null

            agc?.release()
            agc = null

            initialized = false
            currentAudioSessionId = -1
            Timber.d("WebRTC audio processor released")
        } catch (e: Exception) {
            Timber.e(e, "Error releasing WebRTC audio processor")
        }
    }
}

/**
 * Available audio effects.
 */
enum class AudioEffect {
    AEC,  // Acoustic Echo Cancellation
    NS,   // Noise Suppression
    AGC   // Automatic Gain Control
}