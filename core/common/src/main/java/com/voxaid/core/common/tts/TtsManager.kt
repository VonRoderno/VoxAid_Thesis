package com.voxaid.core.common.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for Text-to-Speech functionality.
 * Wraps Android TTS with coroutine-friendly interface.
 */
@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false

    private val _ttsEvents = Channel<TtsEvent>(Channel.BUFFERED)
    val ttsEvents: Flow<TtsEvent> = _ttsEvents.receiveAsFlow()

    init {
        initializeTts()
    }

    private fun initializeTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                textToSpeech?.setSpeechRate(0.9f) // Slightly slower for clarity
                textToSpeech?.setPitch(1.0f)

                // Set utterance progress listener
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Timber.d("TTS started: $utteranceId")
                        _ttsEvents.trySend(TtsEvent.Started)
                    }

                    override fun onDone(utteranceId: String?) {
                        Timber.d("TTS completed: $utteranceId")
                        _ttsEvents.trySend(TtsEvent.Completed)
                    }

                    override fun onError(utteranceId: String?) {
                        Timber.e("TTS error: $utteranceId")
                        _ttsEvents.trySend(TtsEvent.Error("TTS error occurred"))
                    }
                })

                isInitialized = true
                Timber.i("TTS initialized successfully")
                _ttsEvents.trySend(TtsEvent.Initialized)
            } else {
                Timber.e("TTS initialization failed")
                _ttsEvents.trySend(TtsEvent.Error("TTS initialization failed"))
            }
        }
    }

    /**
     * Speaks the given text.
     * @param text The text to speak
     * @param utteranceId Unique ID for this utterance
     * @param queueMode QUEUE_ADD or QUEUE_FLUSH
     */
    fun speak(
        text: String,
        utteranceId: String = UUID.randomUUID().toString(),
        queueMode: Int = TextToSpeech.QUEUE_FLUSH
    ) {
        if (!isInitialized) {
            Timber.w("TTS not initialized, cannot speak")
            return
        }

        textToSpeech?.speak(text, queueMode, null, utteranceId)
        Timber.d("Speaking: $text")
    }

    /**
     * Stops current speech.
     */
    fun stop() {
        textToSpeech?.stop()
        Timber.d("TTS stopped")
    }

    /**
     * Checks if TTS is currently speaking.
     */
    fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking ?: false
    }

    /**
     * Sets speech rate.
     * @param rate 1.0 is normal, < 1.0 is slower, > 1.0 is faster
     */
    fun setSpeechRate(rate: Float) {
        textToSpeech?.setSpeechRate(rate)
        Timber.d("TTS speech rate set to $rate")
    }

    /**
     * Cleans up TTS resources.
     * Call this when TTS is no longer needed.
     */
    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
        Timber.i("TTS shutdown")
    }
}

/**
 * Events emitted by TTS manager.
 */
sealed class TtsEvent {
    data object Initialized : TtsEvent()
    data object Started : TtsEvent()
    data object Completed : TtsEvent()
    data class Error(val message: String) : TtsEvent()
}