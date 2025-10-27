package com.voxaid.core.audio.vosk

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.voxaid.core.audio.AsrManager
import com.voxaid.core.audio.model.VoiceIntent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Vosk-based ASR manager implementation.
 * Uses vosk-model-small-en-us-0.15 for offline speech recognition.
 */
@Singleton
class VoskAsrManager @Inject constructor(
    @ApplicationContext private val context: Context
) : AsrManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _recognizedIntents = Channel<VoiceIntent>(Channel.BUFFERED)
    override val recognizedIntents: Flow<VoiceIntent> = _recognizedIntents.receiveAsFlow()

    private val _isListening = MutableStateFlow(false)
    override val isListening: Flow<Boolean> = _isListening.asStateFlow()

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var initialized = false
    private var recognitionJob: Job? = null

    companion object {
        private const val MODEL_NAME = "vosk-model-small-en-us-0.15"
        private const val SAMPLE_RATE = 16000f
    }

    override suspend fun initialize(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (initialized) {
                    Timber.d("VoskAsrManager already initialized")
                    return@withContext Result.success(Unit)
                }

                // Extract model from assets if not exists
                val modelDir = File(context.filesDir, MODEL_NAME)

                Timber.d("Model directory path: ${modelDir.absolutePath}")
                Timber.d("Model directory exists: ${modelDir.exists()}")

                if (!modelDir.exists() || !isModelValid(modelDir)) {
                    Timber.i("Extracting Vosk model from assets to ${modelDir.absolutePath}")
                    extractModelFromAssets(modelDir)
                } else {
                    Timber.d("Model already extracted, verifying integrity")
                }

                // Verify model files exist
                if (!isModelValid(modelDir)) {
                    throw IOException("Model extraction failed or incomplete. Required files missing in ${modelDir.absolutePath}")
                }

                // Initialize Vosk model
                Timber.d("Loading Vosk model from ${modelDir.absolutePath}")
                model = Model(modelDir.absolutePath)

                initialized = true
                Timber.i("VoskAsrManager initialized successfully")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "VoskAsrManager initialization failed")
                Result.failure(e)
            }
        }
    }

    override fun start() {
        if (!initialized) {
            Timber.w("VoskAsrManager not initialized")
            return
        }

        if (_isListening.value) {
            Timber.d("Already listening")
            return
        }

        try {
            // Create SpeechService with RecognitionListener
            val recognizer = Recognizer(model, SAMPLE_RATE)
            speechService = SpeechService(recognizer, SAMPLE_RATE)

            speechService?.startListening(object : RecognitionListener {
                override fun onResult(hypothesis: String?) {
                    hypothesis?.let { processRecognitionResult(it, isFinal = true) }
                }

                override fun onPartialResult(hypothesis: String?) {
                    hypothesis?.let { processRecognitionResult(it, isFinal = false) }
                }

                override fun onFinalResult(hypothesis: String?) {
                    hypothesis?.let { processRecognitionResult(it, isFinal = true) }
                }

                override fun onError(exception: Exception?) {
                    Timber.e(exception, "Vosk recognition error")
                    _isListening.value = false
                }

                override fun onTimeout() {
                    Timber.d("Vosk recognition timeout")
                }
            })

            _isListening.value = true
            Timber.d("VoskAsrManager started listening")

        } catch (e: Exception) {
            Timber.e(e, "Failed to start VoskAsrManager")
            _isListening.value = false
        }
    }

    override fun stop() {
        speechService?.stop()
        speechService?.shutdown()
        speechService = null

        _isListening.value = false
        Timber.d("VoskAsrManager stopped")
    }

    override fun pause() {
        stop()
    }

    override fun shutdown() {
        stop()

        recognitionJob?.cancel()
        recognitionJob = null

        model?.close()
        model = null

        initialized = false
        Timber.i("VoskAsrManager shutdown")
    }

    override fun isReady(): Boolean = initialized

    /**
     * Process recognition result and extract voice intent.
     */
    private fun processRecognitionResult(jsonResult: String, isFinal: Boolean) {
        try {
            val json = JSONObject(jsonResult)
            val text = json.optString("text", "").trim()

            if (text.isNotEmpty()) {
                Timber.d("Recognized ${if (isFinal) "final" else "partial"}: $text")

                // Only process final results to avoid duplicate commands
                if (isFinal) {
                    val intent = VoiceIntent.fromText(text)
                    scope.launch {
                        _recognizedIntents.send(intent)
                        Timber.i("Emitted voice intent: $intent from text: $text")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error processing recognition result: $jsonResult")
        }
    }

    /**
     * Check if model directory contains all required files.
     */
    private fun isModelValid(modelDir: File): Boolean {
        if (!modelDir.exists() || !modelDir.isDirectory) {
            return false
        }

        // Check for essential model directories/files
        val requiredPaths = listOf(
            "am/final.mdl",
            "conf/mfcc.conf",
            "conf/model.conf",
            "graph/phones/word_boundary.int"
        )

        return requiredPaths.all { path ->
            File(modelDir, path).exists().also { exists ->
                if (!exists) {
                    Timber.w("Missing required model file: $path")
                }
            }
        }
    }

    /**
     * Extract Vosk model from assets to internal storage.
     */
    private fun extractModelFromAssets(targetDir: File) {
        try {
            // Clean up any partial extraction
            if (targetDir.exists()) {
                Timber.d("Cleaning up existing model directory")
                targetDir.deleteRecursively()
            }

            targetDir.mkdirs()

            val assetManager = context.assets

            // Check if model exists in assets
            val modelFiles = try {
                assetManager.list(MODEL_NAME)
            } catch (e: IOException) {
                null
            }

            if (modelFiles == null || modelFiles.isEmpty()) {
                throw IOException(
                    "Vosk model not found in assets/$MODEL_NAME.\n" +
                            "Please download vosk-model-small-en-us-0.15 and place it in:\n" +
                            "app/src/main/assets/$MODEL_NAME/\n" +
                            "See VOSK_SETUP_GUIDE.md for instructions."
                )
            }

            Timber.i("Found model in assets, extracting ${modelFiles.size} items...")

            // Copy all files recursively
            copyAssetFolder(assetManager, MODEL_NAME, targetDir.absolutePath)

            // Verify extraction
            if (!isModelValid(targetDir)) {
                throw IOException("Model extraction completed but validation failed. Some files may be missing.")
            }

            Timber.i("Vosk model extracted and validated successfully")
        } catch (e: IOException) {
            Timber.e(e, "Failed to extract Vosk model")
            // Clean up failed extraction
            targetDir.deleteRecursively()
            throw e
        }
    }

    /**
     * Recursively copy asset folder.
     */
    private fun copyAssetFolder(
        assetManager: android.content.res.AssetManager,
        fromAssetPath: String,
        toPath: String
    ) {
        val files = try {
            assetManager.list(fromAssetPath)
        } catch (e: IOException) {
            Timber.e(e, "Error listing assets at: $fromAssetPath")
            return
        }

        if (files == null || files.isEmpty()) {
            return
        }

        File(toPath).mkdirs()

        for (file in files) {
            val fromPath = if (fromAssetPath.isEmpty()) file else "$fromAssetPath/$file"
            val targetFile = File(toPath, file)

            val subFiles = try {
                assetManager.list(fromPath)
            } catch (e: IOException) {
                null
            }

            if (subFiles != null && subFiles.isNotEmpty()) {
                // It's a directory
                Timber.v("Creating directory: ${targetFile.absolutePath}")
                copyAssetFolder(assetManager, fromPath, targetFile.absolutePath)
            } else {
                // It's a file
                Timber.v("Copying file: $fromPath -> ${targetFile.absolutePath}")
                try {
                    assetManager.open(fromPath).use { input ->
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: IOException) {
                    Timber.e(e, "Failed to copy file: $fromPath")
                    throw e
                }
            }
        }
    }
}