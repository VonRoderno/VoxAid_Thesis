package com.voxaid.feature.instruction.emergency

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voxaid.core.audio.AudioSessionManager
import com.voxaid.core.audio.model.VoiceIntent
import com.voxaid.core.common.datastore.PreferencesManager
import com.voxaid.core.common.tts.TtsManager
import com.voxaid.core.content.model.EmergencyProtocol
import com.voxaid.core.content.model.EmergencyStep
import com.voxaid.core.content.repository.EmergencyProtocolRepository
import com.voxaid.core.content.repository.ProtocolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Emergency Mode instruction screen.
 * Manages emergency step engine, timers, voice commands, and UI state.
 */
@HiltViewModel
class EmergencyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val protocolRepository: ProtocolRepository,
    private val emergencyProtocolRepository: EmergencyProtocolRepository,
    private val ttsManager: TtsManager,
    private val audioSessionManager: AudioSessionManager,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val emergencyId: String = savedStateHandle.get<String>("variant") ?: "emergency_cpr"

    private var stepEngine: EmergencyStepEngine? = null
    private var protocol: EmergencyProtocol? = null

    private val _uiState = MutableStateFlow<EmergencyUiState>(EmergencyUiState.Loading)
    val uiState: StateFlow<EmergencyUiState> = _uiState.asStateFlow()

    private val _currentStep = MutableStateFlow<EmergencyStep?>(null)
    val currentStep: StateFlow<EmergencyStep?> = _currentStep.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0)
    val elapsedTime: StateFlow<Int> = _elapsedTime.asStateFlow()

    private val _beatCount = MutableStateFlow(0)
    val beatCount: StateFlow<Int> = _beatCount.asStateFlow()

    private val _showPopup = MutableStateFlow<PopupData?>(null)
    val showPopup: StateFlow<PopupData?> = _showPopup.asStateFlow()

    private val _showTimerPopup = MutableStateFlow<TimerPopupData?>(null)
    val showTimerPopup: StateFlow<TimerPopupData?> = _showTimerPopup.asStateFlow()

    private val _isMetronomeActive = MutableStateFlow(false)
    val isMetronomeActive: StateFlow<Boolean> = _isMetronomeActive.asStateFlow()

    private val _show911Dialog = MutableStateFlow(false)
    val show911Dialog: StateFlow<Boolean> = _show911Dialog.asStateFlow()

    private val _showVoiceHint = MutableStateFlow<String?>(null)
    val showVoiceHint: StateFlow<String?> = _showVoiceHint.asStateFlow()

    private val _canNavigateManually = MutableStateFlow(true) // Always allow manual navigation
    val canNavigateManually: StateFlow<Boolean> = _canNavigateManually.asStateFlow()

    val audioState = audioSessionManager.audioState

    private var timerJob: Job? = null

    init {
        loadEmergencyProtocol()
        observeVoiceCommands()
        initializeAudio()
    }

    private fun initializeAudio() {
        viewModelScope.launch {
            audioSessionManager.initialize()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeVoiceCommands() {
        viewModelScope.launch {
            audioSessionManager.audioState
                .flatMapLatest { state ->
                    if (state.asrReady) {
                        audioSessionManager.recognizedIntents
                    } else {
                        emptyFlow()
                    }
                }
                .collect { intent ->
                    handleVoiceIntent(intent)
                }
        }
    }

    private fun handleVoiceIntent(intent: VoiceIntent) {
        logVoiceDebug(intent) // ADD THIS LINE

        Timber.d("Emergency voice intent received: $intent")

        when (intent) {
            // Navigation
            is VoiceIntent.NextStep -> {
                stepEngine?.isWaitingForVoiceInput()?.let {
                    if (!it) {
                        nextStep()
                    } else {
                        Timber.d("Ignoring NextStep - waiting for specific voice trigger")
                    }
                }
            }

            is VoiceIntent.PreviousStep -> {
                // Emergency mode doesn't typically go back, but allow it
                previousStep()
            }

            is VoiceIntent.RepeatStep -> repeatStep()

            // Emergency-specific intents
            is VoiceIntent.SafeClear -> {
                Timber.i("Voice: SAFE/CLEAR detected")
                handleEmergencyKeyword("safe")
            }

            is VoiceIntent.Yes -> {
                Timber.i("Voice: YES detected")
                handleEmergencyKeyword("yes")
            }

            is VoiceIntent.No -> {
                Timber.i("Voice: NO detected")
                handleEmergencyKeyword("no")
            }

            is VoiceIntent.Responsive -> {
                Timber.i("Voice: RESPONSIVE detected")
                handleEmergencyKeyword("responsive")
            }

            is VoiceIntent.Unresponsive -> {
                Timber.i("Voice: UNRESPONSIVE detected")
                handleEmergencyKeyword("unresponsive")
            }

            is VoiceIntent.Alone -> {
                Timber.i("Voice: ALONE detected")
                handleEmergencyKeyword("alone")
            }

            is VoiceIntent.NotAlone -> {
                Timber.i("Voice: NOT ALONE detected")
                handleEmergencyKeyword("not alone")
            }

            is VoiceIntent.Continue -> {
                Timber.i("Voice: CONTINUE detected")
                handleEmergencyKeyword("continue")
            }

            is VoiceIntent.Stop -> {
                Timber.i("Voice: STOP detected")
                handleEmergencyKeyword("stop")
            }

            // Other intents
            is VoiceIntent.Call911 -> {
                _show911Dialog.value = true
                Timber.w("Voice command: Call 911 - showing dialog")
            }

            is VoiceIntent.Unknown -> {
                // Try to match against step-specific keywords
                val keyword = intent.rawText.lowercase().trim()
                Timber.d("Unknown voice intent, trying keyword match: '$keyword'")

                val matched = handleEmergencyKeyword(keyword)

                if (!matched) {
                    Timber.w("No match found for voice input: '$keyword'")
                    // Show hint to user
                    _showVoiceHint.value = "Try saying the highlighted keyword clearly"
                }
            }

            else -> {
                Timber.d("Unhandled voice intent in emergency mode: $intent")
            }
        }
    }

    /**
     * Handles emergency-specific keywords.
     * Returns true if keyword was handled, false otherwise.
     */
    private fun handleEmergencyKeyword(keyword: String): Boolean {
        val currentStep = _currentStep.value
        val normalized = keyword.lowercase().trim()

        Timber.d("""
        ╔═══════════════════════════════════════════════════════
        ║ EMERGENCY KEYWORD HANDLING
        ╠═══════════════════════════════════════════════════════
        ║ Keyword: '$normalized'
        ║ Current Step: ${currentStep?.title}
        ║ Step Type: ${currentStep?.javaClass?.simpleName}
        ║ Step ID: ${stepEngine?.currentStepId?.value}
        ╚═══════════════════════════════════════════════════════
    """.trimIndent())

        if (currentStep == null) {
            Timber.e("Cannot handle keyword - no current step")
            return false
        }

        // Log expected keywords for debugging
        when (currentStep) {
            is EmergencyStep.VoiceTrigger -> {
                Timber.d("Expected keywords: ${currentStep.expectedKeywords}")
            }
            is EmergencyStep.Popup -> {
                Timber.d("Expected YES: ${currentStep.yesKeywords}, NO: ${currentStep.noKeywords}")
            }
            else -> {
                Timber.d("Step type doesn't expect voice input")
            }
        }

        val handled = stepEngine?.handleVoiceKeyword(normalized) ?: false

        if (handled) {
            Timber.i("✓✓✓ Emergency keyword '$normalized' SUCCESSFULLY handled ✓✓✓")
            _showVoiceHint.value = null // Clear any hints

            val step = _currentStep.value
            if (step is EmergencyStep.Popup) {
                if (step.yesKeywords.any { it.equals(normalized, ignoreCase = true) }) {
                    Timber.i("Voice matched YES — dismissing popup & continuing")
                    handlePopupYes()
                } else if (step.noKeywords.any { it.equals(normalized, ignoreCase = true) }) {
                    Timber.i("Voice matched NO — dismissing popup & continuing")
                    handlePopupNo()
                }
            }
        } else {
            Timber.w("✗✗✗ Emergency keyword '$normalized' NOT handled ✗✗✗")
            _showVoiceHint.value = "Try saying: ${getExpectedKeywordsHint(currentStep)}"
        }

        return handled
    }

    /**
     * Gets a user-friendly hint of expected keywords.
     */
    private fun getExpectedKeywordsHint(step: EmergencyStep): String {
        return when (step) {
            is EmergencyStep.VoiceTrigger -> step.expectedKeywords.joinToString(" or ").uppercase()
            is EmergencyStep.Popup -> "YES or NO"
            else -> "NEXT or REPEAT"
        }
    }

    fun dismiss911Dialog() {
        _show911Dialog.value = false
    }

    fun show911Dialog() {
        _show911Dialog.value = true
        Timber.i("Call 911 dialog shown via button tap")
    }

    fun startListening() {
        audioSessionManager.startSession()
        Timber.d("Started voice listening (emergency mode)")
    }

    fun stopListening() {
        audioSessionManager.stopSession()
        Timber.d("Stopped voice listening (emergency mode)")
    }

    private fun loadEmergencyProtocol() {
        viewModelScope.launch {
            _uiState.value = EmergencyUiState.Loading

            emergencyProtocolRepository.getEmergencyProtocol(emergencyId)
                .onSuccess { emergencyProtocol ->
                    protocol = emergencyProtocol

                    // Initialize step engine
                    initializeStepEngine(emergencyProtocol)

                    Timber.i("Emergency protocol loaded: ${emergencyProtocol.name}")
                }
                .onFailure { error ->
                    _uiState.value = EmergencyUiState.Error(
                        error.message ?: "Failed to load emergency protocol"
                    )
                    Timber.e(error, "Failed to load emergency protocol")
                }
        }
    }

    /**
     * Initializes the step engine with loaded protocol.
     * Call this after protocol is loaded.
     */
    fun initializeStepEngine(emergencyProtocol: EmergencyProtocol) {
        protocol = emergencyProtocol
        stepEngine = EmergencyStepEngine(emergencyProtocol)

        // Observe step engine state
        viewModelScope.launch {
            stepEngine!!.currentStep.collect { step ->
                _currentStep.value = step
                handleStepTransition(step)
            }
        }

        viewModelScope.launch {
            stepEngine!!.elapsedTime.collect { time ->
                _elapsedTime.value = time
            }
        }

        viewModelScope.launch {
            stepEngine!!.beatCount.collect { count ->
                _beatCount.value = count
            }
        }

        _uiState.value = EmergencyUiState.Success(emergencyProtocol)
        Timber.i("Emergency step engine initialized")
    }

    private fun handleStepTransition(step: EmergencyStep?) {
        step ?: return

        logStepDebug(step)

        // Speak voice prompt
        ttsManager.speak(step.voicePrompt)

        // Handle step-specific logic
        when (step) {
            is EmergencyStep.Popup -> {
                // Show popup dialog
                _showPopup.value = PopupData(
                    title = step.title,
                    message = step.description,
                    yesLabel = "Yes",
                    noLabel = "No"
                )
            }

            is EmergencyStep.Timed -> {
                // Start timer
                startTimer(step.durationSeconds)

                // Start metronome if needed
                if (step.showMetronome) {
                    _isMetronomeActive.value= true
                }
            }
            is EmergencyStep.Terminal -> {
                // Stop all timers and metronome
                stopTimer()
                _isMetronomeActive.value = false
                Timber.i("Reached terminal step: ${step.outcomeType}")
            }

            else -> {
                // Stop timers for non-timed steps
                stopTimer()
                _isMetronomeActive.value = false
            }
        }
    }

    private fun logVoiceDebug(intent: VoiceIntent) {
        when (intent) {
            is VoiceIntent.Unknown -> {
                val currentStep = _currentStep.value
                Timber.w("""
                ╔════════════════════════════════════════════════════════
                ║ VOICE RECOGNITION DEBUG
                ╠════════════════════════════════════════════════════════
                ║ Raw Text: "${intent.rawText}"
                ║ Normalized: "${intent.rawText.lowercase().trim()}"
                ║ Current Step: ${currentStep?.title}
                ║ Step Type: ${currentStep?.javaClass?.simpleName}
                ║ Expected Keywords: ${getExpectedKeywords(currentStep)}
                ╚════════════════════════════════════════════════════════
            """.trimIndent())
            }
            else -> {
                Timber.d("Voice intent recognized: ${intent.javaClass.simpleName}")
            }
        }
    }

    private fun getExpectedKeywords(step: EmergencyStep?): String {
        return when (step) {
            is EmergencyStep.VoiceTrigger -> step.expectedKeywords.joinToString(", ")
            is EmergencyStep.Popup -> "${step.yesKeywords.joinToString(", ")} | ${step.noKeywords.joinToString(", ")}"
            else -> "N/A (not voice-triggered)"
        }
    }

    /**
     * Logs detailed step information for debugging.
     */
    private fun logStepDebug(step: EmergencyStep) {
        val stepInfo = when (step) {
            is EmergencyStep.VoiceTrigger -> """
            Type: VoiceTrigger
            Expected Keywords: ${step.expectedKeywords}
            Timeout: ${step.timeoutSeconds}s
            Next Step ID: ${stepEngine?.getNextStepId(step.stepId)}
        """.trimIndent()

            is EmergencyStep.Popup -> """
            Type: Popup
            YES Keywords: ${step.yesKeywords}
            NO Keywords: ${step.noKeywords}
            YES → ${step.yesNextStepId}
            NO → ${step.noNextStepId}
        """.trimIndent()

            is EmergencyStep.Instruction -> """
            Type: Instruction
            Duration: ${step.durationSeconds}s
            Critical Warning: ${step.criticalWarning}
            Next Step ID: ${stepEngine?.getNextStepId(step.stepId)}
        """.trimIndent()

            is EmergencyStep.Timed -> """
            Type: Timed
            Duration: ${step.durationSeconds}s
            Metronome: ${step.showMetronome} @ ${step.metronomeBpm} BPM
            Count Beats: ${step.countBeats} / ${step.targetBeats}
            Timer Events: ${step.timerEvents.size}
        """.trimIndent()

            is EmergencyStep.Loop -> """
            Type: Loop
            Loop To: ${step.loopToStepId}
            Max Iterations: ${step.maxIterations}
        """.trimIndent()

            is EmergencyStep.Terminal -> """
            Type: Terminal
            Outcome: ${step.outcomeType}
        """.trimIndent()
        }

        Timber.d("""
        ╔═══════════════════════════════════════════════════════
        ║ CURRENT STEP DEBUG
        ╠═══════════════════════════════════════════════════════
        ║ ID: ${step.stepId}
        ║ Title: ${step.title}
        ║ $stepInfo
        ╚═══════════════════════════════════════════════════════
    """.trimIndent())
    }

    private fun startTimer(durationSeconds: Int) {
        stopTimer() // Cancel any existing timer

        timerJob = viewModelScope.launch {
            for (second in 0..durationSeconds) {
                _elapsedTime.value = second
                stepEngine?.updateElapsedTime(second)
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _elapsedTime.value = 0
    }

    /**
     * Manual navigation for emergency mode (fallback when voice fails).
     */
    fun nextStep() {
        Timber.d("Manual next step triggered")
        val success = stepEngine?.nextStep() ?: false
        if (!success) {
            Timber.d("Cannot advance - at terminal or waiting for input")
        }
    }

    fun previousStep() {
        Timber.d("Manual previous step triggered")
        // In emergency mode, going back is allowed but logged
        val currentStepId = stepEngine?.currentStepId?.value
        Timber.w("User went back in emergency mode from step: $currentStepId")

        // For now, just repeat the step as "back" isn't typical in emergency flow
        repeatStep()
    }

    /**
     * Manual popup selection (when voice fails).
     */
    fun selectPopupYesManually() {
        Timber.d("Manual YES button pressed")
        handlePopupYes()
    }

    fun selectPopupNoManually() {
        Timber.d("Manual NO button pressed")
        handlePopupNo()
    }

    fun repeatStep() {
        val currentStep = _currentStep.value
        if (currentStep != null) {
            ttsManager.speak(currentStep.voicePrompt)
            Timber.d("Repeating step: ${currentStep.title}")
        }
    }

    fun handlePopupYes() {
        _showPopup.value = null
        stepEngine?.handlePopupSelection(isYes = true)
    }

    fun handlePopupNo() {
        _showPopup.value = null
        stepEngine?.handlePopupSelection(isYes = false)
    }

    fun dismissPopup() {
        _showPopup.value = null
    }

    fun handleTimerPopupResponse(response: TimerPopupResponse) {
        _showTimerPopup.value = null

        when (response) {
            TimerPopupResponse.Continue -> {
                // Continue current step
                Timber.d("User chose to continue")
            }
            TimerPopupResponse.Switch -> {
                // Navigate to switch step
                stepEngine?.goToStep("switch_rescuers")
            }
            TimerPopupResponse.Rest -> {
                // Navigate to exhausted terminal
                stepEngine?.goToStep("exhausted_terminal")
            }
        }
    }

    fun onMetronomeBeat() {
        val current = _beatCount.value
        _beatCount.value = current + 1
        stepEngine?.updateBeatCount(current + 1)
    }

    fun stopTts() {
        ttsManager.stop()
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.stop()
        stopListening()
        stopTimer()
        _isMetronomeActive.value = false
    }
}
/**

UI state for emergency mode.
 */
sealed class EmergencyUiState {
    data object Loading : EmergencyUiState()
    data class Success(val protocol: EmergencyProtocol) : EmergencyUiState()
    data class Error(val message: String) : EmergencyUiState()
}

/**

Popup dialog data.
 */
data class PopupData(
    val title: String,
    val message: String,
    val yesLabel: String,
    val noLabel: String
)

/**

Timer-triggered popup data.
 */
data class TimerPopupData(
    val title: String,
    val message: String,
    val options: List<TimerPopupOption>
)

data class TimerPopupOption(
    val label: String,
    val response: TimerPopupResponse
)
enum class TimerPopupResponse {
    Continue,
    Switch,
    Rest
}
