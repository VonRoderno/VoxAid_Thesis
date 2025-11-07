package com.voxaid.core.audio

import com.voxaid.core.audio.model.VoiceIntent
import kotlinx.coroutines.flow.Flow

/**
 * Interface for Automatic Speech Recognition manager.
 * Abstracts the ASR implementation (Vosk, Google, etc.) for testability.
 */
interface AsrManager {

    /**
     * Flow of recognized voice intents.
     * Emits whenever speech is recognized and mapped to an intent.
     */
    val recognizedIntents: Flow<VoiceIntent>

    /**
     * Flow indicating if ASR is actively listening.
     */
    val isListening: Flow<Boolean>

    fun currentListeningState(): Boolean

    /**
     * Initializes the ASR engine.
     * Must be called before start().
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Starts listening for speech.
     * Requires microphone permission.
     */
    fun start()

    /**
     * Stops listening for speech.
     */
    fun stop()

    /**
     * Pauses listening temporarily.
     * Can be resumed with start().
     */
    fun pause()

    /**
     * Releases ASR resources.
     * Call when ASR is no longer needed.
     */
    fun shutdown()

    /**
     * Checks if ASR is initialized and ready.
     */
    fun isReady(): Boolean
}