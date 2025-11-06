// feature/instruction/src/main/java/com/voxaid/feature/instruction/emergency/EmergencyViewModel.kt
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EmergencyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
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

    // ==================== CPR TIMER STATE ====================

    /**
     * Elapsed time in compression cycle (resets every 2 minutes or on switch)
     */
    private val _compressionCycleTime = MutableStateFlow(0)
    val compressionCycleTime: StateFlow<Int> = _compressionCycleTime.asStateFlow()

    /**
     * Show 1:40 warning banner
     */
    private val _showSwitchWarning = MutableStateFlow(false)
    val showSwitchWarning: StateFlow<Boolean> = _showSwitchWarning.asStateFlow()

    /**
     * Show 2-minute rescuer switch dialog
     */
    private val _showRescuerDialog = MutableStateFlow(false)
    val showRescuerDialog: StateFlow<Boolean> = _showRescuerDialog.asStateFlow()

    /**
     * Show exhaustion check for solo rescuers
     */
    private val _showContinueDialog = MutableStateFlow(false)
    val showContinueDialog: StateFlow<Boolean> = _showContinueDialog.asStateFlow()

    /**
     * Show switching rescuer overlay (5 seconds)
     */
    private val _showSwitchOverlay = MutableStateFlow(false)
    val showSwitchOverlay: StateFlow<Boolean> = _showSwitchOverlay.asStateFlow()

    /**
     * Show final exhaustion message
     */
    private val _showExhaustionMessage = MutableStateFlow(false)
    val showExhaustionMessage: StateFlow<Boolean> = _showExhaustionMessage.asStateFlow()

    private var compressionTimerJob: Job? = null

    // ==================== EXISTING STATE ====================

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

    val audioState = audioSessionManager.audioState

    private var timerJob: Job? = null

    init {
        loadEmergencyProtocol()
        observeVoiceCommands()
        initializeAudio()
    }

    // ==================== CPR TIMER METHODS ====================

    /**
     * Starts the compression cycle timer.
     * Called when entering "Chest Compression" step.
     *
     * Timeline:
     * - 0:00 - 1:40: Normal compressions
     * - 1:40: Show warning banner + subtle cue
     * - 2:00: Pause and show rescuer dialog
     */
    private fun startCompressionCycleTimer() {
        stopCompressionCycleTimer()

        _compressionCycleTime.value = 0
        _showSwitchWarning.value = false

        compressionTimerJob = viewModelScope.launch {
            while (_compressionCycleTime.value < 120) { // 2 minutes
                delay(1000)
                _compressionCycleTime.value++

                val elapsed = _compressionCycleTime.value

                when (elapsed) {
                    100 -> {
                        // 1:40 - Show warning
                        _showSwitchWarning.value = true
                        ttsManager.speak("Prepare to switch soon.")
                        playSwitchWarningCue()
                        Timber.d("CPR Timer: 1:40 warning triggered")
                    }
                    120 -> {
                        // 2:00 - Pause and show dialog
                        pauseCompressions()
                        _showRescuerDialog.value = true
                        ttsManager.speak("Two minutes elapsed. Are you with someone?")
                        Timber.d("CPR Timer: 2:00 rescuer check triggered")
                    }
                }
            }
        }
    }

    /**
     * Stops and resets compression timer.
     */
    private fun stopCompressionCycleTimer() {
        compressionTimerJob?.cancel()
        compressionTimerJob = null
        _compressionCycleTime.value = 0
        _showSwitchWarning.value = false
    }

    /**
     * Resets timer and resumes compressions.
     * Called after switching rescuers or choosing to continue.
     */
    private fun resetAndResumeCompressions() {
        _showRescuerDialog.value = false
        _showContinueDialog.value = false
        _showSwitchOverlay.value = false
        _showSwitchWarning.value = false

        // Reset timer
        stopCompressionCycleTimer()

        // Resume compressions
        resumeCompressions()
        startCompressionCycleTimer()

        Timber.d("CPR Timer: Reset and resumed compressions")
    }

    /**
     * Pauses metronome and voice guidance during dialogs.
     */
    private fun pauseCompressions() {
        _isMetronomeActive.value = false
        ttsManager.stop()
    }

    /**
     * Resumes metronome and voice guidance.
     */
    private fun resumeCompressions() {
        _isMetronomeActive.value = true
        ttsManager.speak("Resume chest compressions.")
    }

    /**
     * Plays subtle audio cue at 1:40.
     * Uses a brief, calm tone instead of jarring beep.
     */
    private fun playSwitchWarningCue() {
        // TODO: Implement gentle audio cue
        // Could use short ascending tone or subtle chime
        // Avoid harsh beeps - keep it calm and reassuring

        // Placeholder: Use system notification sound or haptic feedback
        // In production: Load custom audio asset
        Timber.d("Playing switch warning audio cue")
    }

    // ==================== DIALOG HANDLERS ====================

    /**
     * User confirms they are with someone.
     * Show 5-second switch overlay, then resume.
     */
    fun onWithHelp() {
        _showRescuerDialog.value = false
        _showSwitchOverlay.value = true

        ttsManager.speak("Switch rescuers now. Transition quickly.")

        viewModelScope.launch {
            delay(5000) // 5-second switch window
            resetAndResumeCompressions()
        }
    }

    /**
     * User confirms they are alone.
     * Show exhaustion check dialog.
     */
    fun onAlone() {
        _showRescuerDialog.value = false
        _showContinueDialog.value = true

        ttsManager.speak("Can you continue giving chest compressions?")
    }

    /**
     * Solo rescuer chooses to continue.
     * Reset timer and resume.
     */
fun onContinueCompressions() {
        _showContinueDialog.value = false
        resetAndResumeCompressions()

        ttsManager.speak("Good. Continue chest compressions.")
        Timber.d("Solo rescuer continuing compressions")
    }

    /**
     * Solo rescuer is exhausted and needs to stop.
     * Show final encouragement message.
     */
    fun onStopExhausted() {
        _showContinueDialog.value = false
        _showExhaustionMessage.value = true
        stopCompressionCycleTimer()

        _isMetronomeActive.value = false

        ttsManager.speak("Take a break if you are physically exhausted and wait for help. You did what you could. Good job.")
        Timber.d("Solo rescuer stopped - exhausted")
    }

    /**
     * End session from exhaustion message.
     */
    fun onEndSession() {
        _showExhaustionMessage.value = false
        stopListening()
        // Navigation will be handled by screen
    }

    // ==================== EXISTING METHODS (Updated) ====================

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
        Timber.d("Emergency voice intent received: $intent")

        // Handle dialog-specific voice commands
        if (_showRescuerDialog.value) {
            when (intent) {
                is VoiceIntent.Yes -> onWithHelp()
                is VoiceIntent.No -> onAlone()
                is VoiceIntent.NotAlone -> onWithHelp()
                is VoiceIntent.Alone -> onAlone()
                else -> {}
            }
            return
        }

        if (_showContinueDialog.value) {
            when (intent) {
                is VoiceIntent.Yes, is VoiceIntent.Continue -> onContinueCompressions()
                is VoiceIntent.No, is VoiceIntent.Stop -> onStopExhausted()
                else -> {}
            }
            return
        }

        // Standard navigation commands
        when (intent) {
            is VoiceIntent.NextStep -> {
                stepEngine?.isWaitingForVoiceInput()?.let {
                    if (!it) {
                        nextStep()
                    }
                }
            }
            is VoiceIntent.PreviousStep -> previousStep()
            is VoiceIntent.RepeatStep -> repeatStep()
            is VoiceIntent.SafeClear -> handleEmergencyKeyword("safe")
            is VoiceIntent.Yes -> handleEmergencyKeyword("yes")
            is VoiceIntent.No -> handleEmergencyKeyword("no")
            is VoiceIntent.Responsive -> handleEmergencyKeyword("responsive")
            is VoiceIntent.Unresponsive -> handleEmergencyKeyword("unresponsive")
            is VoiceIntent.Alone -> handleEmergencyKeyword("alone")
            is VoiceIntent.NotAlone -> handleEmergencyKeyword("not alone")
            is VoiceIntent.Continue -> handleEmergencyKeyword("continue")
            is VoiceIntent.Stop -> handleEmergencyKeyword("stop")
            is VoiceIntent.Call911 -> {
                _show911Dialog.value = true
                Timber.w("Voice command: Call 911 - showing dialog")
            }
            is VoiceIntent.Unknown -> {
                val keyword = intent.rawText.lowercase().trim()
                val matched = handleEmergencyKeyword(keyword)
                if (!matched) {
                    _showVoiceHint.value = "Try saying the highlighted keyword clearly"
                }
            }
            else -> Timber.d("Unhandled voice intent in emergency mode: $intent")
        }
    }

    private fun handleEmergencyKeyword(keyword: String): Boolean {
        val currentStep = _currentStep.value
        val normalized = keyword.lowercase().trim()

        if (currentStep == null) {
            Timber.e("Cannot handle keyword - no current step")
            return false
        }

        val handled = stepEngine?.handleVoiceKeyword(normalized) ?: false

        if (handled) {
            Timber.i("✓ Emergency keyword '$normalized' handled")
            _showVoiceHint.value = null

            val step = _currentStep.value
            if (step is EmergencyStep.Popup) {
                if (step.yesKeywords.any { it.equals(normalized, ignoreCase = true) }) {
                    handlePopupYes()
                } else if (step.noKeywords.any { it.equals(normalized, ignoreCase = true) }) {
                    handlePopupNo()
                }
            }
        } else {
            Timber.w("✗ Emergency keyword '$normalized' NOT handled")
            _showVoiceHint.value = "Try saying: ${getExpectedKeywordsHint(currentStep)}"
        }

        return handled
    }

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

    fun initializeStepEngine(emergencyProtocol: EmergencyProtocol) {
        protocol = emergencyProtocol
        stepEngine = EmergencyStepEngine(emergencyProtocol)

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


        // Start compression timer only once when entering compression step
        if ((step.stepId == "chest_compressions" ||
                    (step is EmergencyStep.Timed && step.title.contains("Compression", ignoreCase = true)))
            && compressionTimerJob == null // prevent restarting
        ) {
            startCompressionCycleTimer()
            Timber.d("Entered compression step - timer started")
        }
//        // Check if we're entering chest compression step
//        if (step.stepId == "chest_compressions" ||
//            (step is EmergencyStep.Timed && step.title.contains("Compression", ignoreCase = true))) {
//            startCompressionCycleTimer()
//            Timber.d("Entered compression step - timer started")
//        } else {
//            // Stop timer if leaving compression step
//            if (_compressionCycleTime.value > 0) {
//                stopCompressionCycleTimer()
//                Timber.d("Left compression step - timer stopped")
//            }
//        }

        ttsManager.speak(step.voicePrompt)

        when (step) {
            is EmergencyStep.Popup -> {
                _showPopup.value = PopupData(
                    title = step.title,
                    message = step.description,
                    yesLabel = "Yes",
                    noLabel = "No"
                )
            }
            is EmergencyStep.Timed -> {
                startTimer(step.durationSeconds)
                if (step.showMetronome) {
                    _isMetronomeActive.value = true
                }
            }
            is EmergencyStep.Terminal -> {
                stopTimer()
                stopCompressionCycleTimer()
                _isMetronomeActive.value = false
                Timber.i("Reached terminal step: ${step.outcomeType}")
            }
            else -> {
                stopTimer()
                _isMetronomeActive.value = false
            }
        }
    }

    private fun startTimer(durationSeconds: Int) {
        stopTimer()

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

    fun nextStep() {
        Timber.d("Manual next step triggered")
        val success = stepEngine?.nextStep() ?: false
        if (!success) {
            Timber.d("Cannot advance - at terminal or waiting for input")
        }
    }

    fun previousStep() {
        Timber.d("Manual previous step triggered")
        val success = stepEngine?.previousStep() ?: false
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
                Timber.d("User chose to continue")
            }
            TimerPopupResponse.Switch -> {
                stepEngine?.goToStep("switch_rescuers")
            }
            TimerPopupResponse.Rest -> {
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
        stopCompressionCycleTimer()
        _isMetronomeActive.value = false
    }
}

// ==================== UI STATE CLASSES ====================

sealed class EmergencyUiState {
    data object Loading : EmergencyUiState()
    data class Success(val protocol: EmergencyProtocol) : EmergencyUiState()
    data class Error(val message: String) : EmergencyUiState()
}

data class PopupData(
    val title: String,
    val message: String,
    val yesLabel: String,
    val noLabel: String
)

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