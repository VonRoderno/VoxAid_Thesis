package com.voxaid.core.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.voxaid.core.audio.model.AudioConfig
import com.voxaid.core.audio.model.AudioState
import com.voxaid.core.audio.vosk.VoskAsrManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages audio session lifecycle and configuration.
 * Handles audio focus, routing, and lifecycle events.
 * Integrates WebRTC audio processing for noise suppression.
 *
 * 🔧 FIXED: Added TTS coordination to prevent feedback loop
 */
@Singleton
class AudioSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val asrManager: AsrManager,
    private val webRtcProcessor: WebRtcAudioProcessor
) : DefaultLifecycleObserver {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    val recognizedIntents = asrManager.recognizedIntents

    private val _audioState = MutableStateFlow(AudioState())
    val audioState: StateFlow<AudioState> = _audioState.asStateFlow()

    private var audioConfig = AudioConfig()
    private var isSessionActive = false

    // 🔧 NEW: Track if we're currently paused due to TTS
    private var isPausedForTts = false

    /**
     * Initializes the audio session with WebRTC processing.
     */
    suspend fun initialize(config: AudioConfig = AudioConfig()): Result<Unit> {
        return try {
            audioConfig = config

            // Initialize ASR
            asrManager.initialize()
                .onSuccess {
                    _audioState.value = _audioState.value.copy(asrReady = true)
                    Timber.i("Audio session initialized with WebRTC processing")
                }
                .onFailure { error ->
                    _audioState.value = _audioState.value.copy(
                        errorMessage = "ASR initialization failed: ${error.message}"
                    )
                    Timber.e(error, "ASR initialization failed")
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Audio session initialization failed")
            Result.failure(e)
        }
    }

    /**
     * Starts the audio session and ASR with WebRTC processing.
     */
    fun startSession() {
        // Check microphone permission
        val permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {
            Timber.e("Microphone permission denied. Cannot start audio session.")
            _audioState.value = _audioState.value.copy(
                micPermissionGranted = false,
                isRecording = false,
                isListening = false
            )
            return
        }

        // Update mic permission state
        _audioState.value = _audioState.value.copy(micPermissionGranted = true)

        // Request audio focus
        requestAudioFocus()

        // Initialize WebRTC audio processing
        val webRtcResult = webRtcProcessor.initialize(0, audioConfig)
        if (webRtcResult.isSuccess) {
            val effectStatus = webRtcProcessor.getEffectStatus()
            Timber.i("WebRTC effects status: $effectStatus")
        }

        asrManager.start()

        // Only mark listening if ASR really started
        val isAsrListening = asrManager.currentListeningState()

        if (isAsrListening) {
            isSessionActive = true
            isPausedForTts = false // 🔧 Clear TTS pause flag
            _audioState.value = _audioState.value.copy(
                isRecording = true,
                isListening = true
            )
            Timber.i("Audio session started with noise suppression")
        } else {
            Timber.w("ASR failed to start — mic may be unavailable or permission denied.")
            _audioState.value = _audioState.value.copy(
                isRecording = false,
                isListening = false
            )
        }
    }

    /**
     * Stops the audio session and ASR.
     */
    fun stopSession() {
        if (!isSessionActive) {
            return
        }

        asrManager.stop()
        webRtcProcessor.release()

        // Abandon audio focus
        abandonAudioFocus()

        isSessionActive = false
        isPausedForTts = false // 🔧 Clear TTS pause flag
        _audioState.value = _audioState.value.copy(
            isRecording = false,
            isListening = false
        )

        Timber.i("Audio session stopped")
    }

    /**
     * Pauses the session temporarily.
     */
    fun pauseSession() {
        asrManager.pause()
        _audioState.value = _audioState.value.copy(isListening = false)
        Timber.d("Audio session paused")
    }

    /**
     * Resumes a paused session.
     */
    fun resumeSession() {
        asrManager.start()
        _audioState.value = _audioState.value.copy(isListening = true)
        Timber.d("Audio session resumed")
    }

    // 🔧 NEW: Pause ASR specifically for TTS output
    /**
     * Pauses ASR to prevent TTS feedback loop.
     * Call this when TTS starts speaking.
     */
    fun pauseForTts() {
        if (!isSessionActive) {
            Timber.d("Session not active, skipping TTS pause")
            return
        }

        if (isPausedForTts) {
            Timber.d("Already paused for TTS")
            return
        }

        Timber.d("⏸️ Pausing ASR for TTS output")
        asrManager.pause()
        isPausedForTts = true
        _audioState.value = _audioState.value.copy(isListening = false)
    }

    // 🔧 NEW: Resume ASR after TTS completes
    /**
     * Resumes ASR after TTS completes.
     * Call this when TTS finishes speaking.
     */
    fun resumeAfterTts() {
        if (!isSessionActive) {
            Timber.d("Session not active, skipping TTS resume")
            return
        }

        if (!isPausedForTts) {
            Timber.d("Not paused for TTS, skipping resume")
            return
        }

        Timber.d("▶️ Resuming ASR after TTS")
        asrManager.start()
        isPausedForTts = false
        _audioState.value = _audioState.value.copy(isListening = true)
    }

    /**
     * Requests audio focus for voice recording.
     */
    private fun requestAudioFocus() {
        // For API 26+, use AudioFocusRequest
        // For simplicity, we'll just log here
        // In production, implement proper AudioFocusRequest
        Timber.d("Audio focus requested")
    }

    /**
     * Abandons audio focus.
     */
    private fun abandonAudioFocus() {
        Timber.d("Audio focus abandoned")
    }

    /**
     * Lifecycle observer methods
     */
    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        pauseSession()
        Timber.d("Audio session paused (lifecycle)")
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        if (isSessionActive && !isPausedForTts) {
            resumeSession()
            Timber.d("Audio session resumed (lifecycle)")
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        stopSession()
        asrManager.shutdown()
        webRtcProcessor.release()
        Timber.d("Audio session destroyed (lifecycle)")
    }

    /**
     * Checks if microphone permission is granted.
     */
    fun checkMicrophonePermission(granted: Boolean) {
        _audioState.value = _audioState.value.copy(micPermissionGranted = granted)
    }
}