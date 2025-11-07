package com.voxaid.core.audio

import com.voxaid.core.audio.model.VoiceIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Development ASR manager for testing without real speech recognition.
 * Simulates voice commands via coroutines for development and testing.
 *
 * Usage in development:
 * - Call simulateCommand() to trigger voice intents programmatically
 * - Use simulateCommandSequence() for automated testing scenarios
 */
@Singleton
class DevAsrManager @Inject constructor() : AsrManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _recognizedIntents = Channel<VoiceIntent>(Channel.BUFFERED)
    override val recognizedIntents: Flow<VoiceIntent> = _recognizedIntents.receiveAsFlow()

    private val _isListening = MutableStateFlow(false)
    override val isListening: Flow<Boolean> = _isListening.asStateFlow()

    private var initialized = false
    override fun currentListeningState(): Boolean = _isListening.value
    override suspend fun initialize(): Result<Unit> {
        return try {
            delay(500) // Simulate initialization delay
            initialized = true
            Timber.i("DevAsrManager initialized (stub)")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "DevAsrManager initialization failed")
            Result.failure(e)
        }
    }

    override fun start() {
        if (!initialized) {
            Timber.w("DevAsrManager not initialized")
            return
        }

        _isListening.value = true
        Timber.d("DevAsrManager started listening (stub)")

        // Simulate periodic recognition for demo
        scope.launch {
            delay(3000)
            simulateCommand("next")
        }
    }

    override fun stop() {
        _isListening.value = false
        Timber.d("DevAsrManager stopped listening")
    }

    override fun pause() {
        _isListening.value = false
        Timber.d("DevAsrManager paused")
    }

    override fun shutdown() {
        _isListening.value = false
        initialized = false
        Timber.i("DevAsrManager shutdown")
    }

    override fun isReady(): Boolean = initialized

    /**
     * Simulates a voice command for development/testing.
     * @param commandText The text to recognize (e.g., "next", "repeat", "back")
     */
    fun simulateCommand(commandText: String) {
        if (!_isListening.value) {
            Timber.w("Cannot simulate command - not listening")
            return
        }

        scope.launch {
            val intent = VoiceIntent.fromText(commandText)
            _recognizedIntents.send(intent)
            Timber.d("Simulated voice command: $commandText -> $intent")
        }
    }

    /**
     * Simulates a sequence of commands with delays.
     * Useful for automated testing scenarios.
     *
     * Example:
     * ```
     * simulateCommandSequence(
     *     "next" to 2000L,
     *     "repeat" to 3000L,
     *     "next" to 2000L
     * )
     * ```
     */
    fun simulateCommandSequence(vararg commands: Pair<String, Long>) {
        scope.launch {
            for ((command, delayMs) in commands) {
                delay(delayMs)
                if (_isListening.value) {
                    simulateCommand(command)
                }
            }
        }
    }
}