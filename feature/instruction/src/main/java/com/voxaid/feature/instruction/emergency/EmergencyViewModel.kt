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

    private val emergencyId: String = savedStateHandle.get<String>("protocol") ?: "emergency_cpr"

    private var stepEngine: EmergencyStepEngine? = null
    private var protocol: EmergencyProtocol? = null

    private val _uiState = MutableStateFlow<EmergencyUiState>(EmergencyUiState.Loading)
    val uiState: StateFlow<EmergencyUiState> = _uiState.asStateFlow()

    private val _currentStep = MutableStateFlow<EmergencyStep?>(null)
    val currentStep: StateFlow<EmergencyStep?> = _currentStep.asStateFlow()

    // CPR Timer State
    private val _compressionCycleTime = MutableStateFlow(0)
    val compressionCycleTime: StateFlow<Int> = _compressionCycleTime.asStateFlow()

    private val _showSwitchWarning = MutableStateFlow(false)
    val showSwitchWarning: StateFlow<Boolean> = _showSwitchWarning.asStateFlow()

    private val _showRescuerDialog = MutableStateFlow(false)
    val showRescuerDialog: StateFlow<Boolean> = _showRescuerDialog.asStateFlow()

    private val _showContinueDialog = MutableStateFlow(false)
    val showContinueDialog: StateFlow<Boolean> = _showContinueDialog.asStateFlow()

    private val _showSwitchOverlay = MutableStateFlow(false)
    val showSwitchOverlay: StateFlow<Boolean> = _showSwitchOverlay.asStateFlow()

    private val _showExhaustionMessage = MutableStateFlow(false)
    val showExhaustionMessage: StateFlow<Boolean> = _showExhaustionMessage.asStateFlow()

    private val _showTerminalMessage = MutableStateFlow(false)
    val showTerminalMessage: StateFlow<Boolean> = _showTerminalMessage.asStateFlow()

    private val _heimlichPath = MutableStateFlow<String?>(null)
    val heimlichPath: StateFlow<String?> = _heimlichPath.asStateFlow()

    private val _showPathSelection = MutableStateFlow(false)
    val showPathSelection: StateFlow<Boolean> = _showPathSelection.asStateFlow()

    private val _showLoopDialog = MutableStateFlow(false)
    val showLoopDialog: StateFlow<Boolean> = _showLoopDialog.asStateFlow()

    private val _showSuccessDialog = MutableStateFlow(false)
    val showSuccessDialog: StateFlow<Boolean> = _showSuccessDialog.asStateFlow()

    private var loopCount = 0
    private val MAX_LOOPS = 10

    private var compressionTimerJob: Job? = null

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

    val ttsEnabled = preferencesManager.ttsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private var timerJob: Job? = null

    init {
        loadEmergencyProtocol()
        observeVoiceCommands()
        observeTtsState()
        initializeAudio()

        if (emergencyId == "emergency_heimlich") {
            _showPathSelection.value = true
        }
    }

    // CPR Timer Methods
    private fun startCompressionCycleTimer() {
        stopCompressionCycleTimer()

        _compressionCycleTime.value = 0
        _showSwitchWarning.value = false

        compressionTimerJob = viewModelScope.launch {
            while (_compressionCycleTime.value < 120) {
                delay(1000)
                _compressionCycleTime.value++

                val elapsed = _compressionCycleTime.value

                when (elapsed) {
                    100 -> {
                        _showSwitchWarning.value = true
                        // 🔧 UPDATED: Only speak if TTS enabled
                        if (ttsEnabled.value) {
                            ttsManager.speak("Prepare to switch soon.")
                        }
                        playSwitchWarningCue()
                        Timber.d("CPR Timer: 1:40 warning triggered")
                    }

                    120 -> {
                        pauseCompressions()
                        stopCompressionCycleTimer()
                        _showRescuerDialog.value = true
                        // 🔧 UPDATED: Only speak if TTS enabled
                        if (ttsEnabled.value) {
                            ttsManager.speak("Two minutes elapsed. Are you with someone?")
                        }
                        Timber.d("CPR Timer: 2:00 rescuer check triggered - timer stopped")
                    }
                }
            }
        }
    }

    private fun stopCompressionCycleTimer() {
        compressionTimerJob?.cancel()
        compressionTimerJob = null
        _compressionCycleTime.value = 0
        _showSwitchWarning.value = false
    }

    private fun resetAndResumeCompressions() {
        _showRescuerDialog.value = false
        _showContinueDialog.value = false
        _showSwitchOverlay.value = false
        _showSwitchWarning.value = false

        stopCompressionCycleTimer()
        resumeCompressions()
        startCompressionCycleTimer()

        Timber.d("CPR Timer: Reset and resumed compressions")
    }

    private fun pauseCompressions() {
        _isMetronomeActive.value = false
        ttsManager.stop()
    }

    private fun showTerminalmessage() {
        _showTerminalMessage.value = true
    }
    private fun resumeCompressions() {
        _isMetronomeActive.value = true
        // 🔧 UPDATED: Only speak if TTS enabled
        if (ttsEnabled.value) {
            ttsManager.speak("Continue chest compressions")
        }
    }

    private fun playSwitchWarningCue() {
        Timber.d("Playing switch warning audio cue")
    }

    // Dialog Handlers
    fun onWithHelp() {
        _showRescuerDialog.value = false
        _showSwitchOverlay.value = true

        // 🔧 UPDATED: Only speak if TTS enabled
        if (ttsEnabled.value) {
            ttsManager.speak("Switch rescuers now. Transition quickly.")
        }

        viewModelScope.launch {
            delay(5000)
            resetAndResumeCompressions()
        }
    }

    fun onAlone() {
        _showRescuerDialog.value = false
        _showContinueDialog.value = true

        // 🔧 UPDATED: Only speak if TTS enabled
        if (ttsEnabled.value) {
            ttsManager.speak("Can you continue giving chest compressions?")
        }
    }

    fun onContinueCompressions() {
        _showContinueDialog.value = false
        resetAndResumeCompressions()

        // 🔧 UPDATED: Only speak if TTS enabled
        if (ttsEnabled.value) {
            ttsManager.speak("Good. Continue chest compressions.")
        }
        Timber.d("Solo rescuer continuing compressions")
    }

    fun onStopExhausted() {
        _showContinueDialog.value = false
        _showExhaustionMessage.value = true
        stopCompressionCycleTimer()

        _isMetronomeActive.value = false

        // 🔧 UPDATED: Only speak if TTS enabled
        if (ttsEnabled.value) {
            ttsManager.speak("Take a break if you are physically exhausted and wait for help. You did what you could. Good job.")
        }
        Timber.d("Solo rescuer stopped - exhausted")
    }

    fun onEndSession() {
        _showExhaustionMessage.value = false
        stopListening()
    }

    private fun initializeAudio() {
        viewModelScope.launch {
            audioSessionManager.initialize()
        }
    }

    private fun observeTtsState() {
        viewModelScope.launch {
            ttsManager.isSpeaking.collect { isSpeaking ->
                if (isSpeaking) {
                    // TTS started speaking → pause ASR
                    audioSessionManager.pauseForTts()
                    Timber.d("🔇 ASR paused - TTS started")
                } else {
                    // TTS stopped speaking → resume ASR if TTS is enabled
                    if (ttsEnabled.value) {
                        audioSessionManager.resumeAfterTts()
                        Timber.d("🔊 ASR resumed - TTS stopped")
                    }
                }
            }
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

    private var lastTtsCompletionTime = 0L
    private val TTS_COOLDOWN_MS = 500L

    private fun handleVoiceIntent(intent: VoiceIntent) {
        Timber.d("Emergency voice intent received: $intent")

        if (ttsEnabled.value) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTtsCompletionTime < TTS_COOLDOWN_MS) {
                Timber.d("⏳ Ignoring voice command during TTS cooldown")
                return
            }

            if (ttsManager.isSpeaking.value) {
                Timber.d("🔇 Ignoring voice command - TTS is speaking")
                return
            }
        }

        if (isDialogActive()) {
            handleDialogVoiceCommand(intent)
            return
        }

        if (_showSwitchOverlay.value) {
            Timber.d("Ignoring voice command during rescuer switch overlay")
            return
        }

        when (intent) {
            is VoiceIntent.NextStep -> {
                if (!stepEngine?.isWaitingForVoiceInput()!!) {
                    nextStep()
                } else {
                    Timber.d("Ignored NextStep - waiting for voice trigger")
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

    private fun isDialogActive(): Boolean {
        return _showRescuerDialog.value ||
                _showContinueDialog.value ||
                _showExhaustionMessage.value ||
                _showPopup.value != null ||
                _show911Dialog.value ||
                _showTerminalMessage.value
    }

    private fun handleDialogVoiceCommand(intent: VoiceIntent) {
        when {
            _showRescuerDialog.value -> {
                when (intent) {
                    is VoiceIntent.Yes, is VoiceIntent.NotAlone -> onWithHelp()
                    is VoiceIntent.No, is VoiceIntent.Alone -> onAlone()
                    else -> Timber.d("Ignored command during rescuer dialog: $intent")
                }
            }

            _showContinueDialog.value -> {
                when (intent) {
                    is VoiceIntent.Yes, is VoiceIntent.Continue -> onContinueCompressions()
                    is VoiceIntent.No, is VoiceIntent.Stop -> onStopExhausted()
                    else -> Timber.d("Ignored command during continue dialog: $intent")
                }
            }

            _showPopup.value != null -> {  // ✅ Added handler
                when (intent) {
                    is VoiceIntent.Yes -> handlePopupYes()
                    is VoiceIntent.No -> handlePopupNo()
                    else -> Timber.d("Ignored command during popup: $intent")
                }
            }

            else -> {
                Timber.d("Dialog active but no specific handler - ignored: $intent")
            }
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
        } else {
            Timber.w("✗ Emergency keyword '$normalized' NOT handled")
            _showVoiceHint.value = "Try saying: ${getExpectedKeywordsHint(currentStep)}"
        }

        return handled
    }

    private fun getExpectedKeywordsHint(step: EmergencyStep): String {
        return when (step) {
            is EmergencyStep.VoiceTrigger -> {
                if (step.stepId == "tap_shout") {
                    "YES or NO"
                } else {
                    step.expectedKeywords.joinToString(" or ").uppercase()
                }
            }

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

        if ((step.stepId == "chest_compressions" ||
                    (step is EmergencyStep.Timed && step.title.contains(
                        "Compression",
                        ignoreCase = true
                    )))
            && compressionTimerJob == null
        ) {
            startCompressionCycleTimer()
            Timber.d("Entered compression step - timer started")
        }
        if (step.title == "Call Emergency"){
            _show911Dialog.value = true
            Timber.w("Reached call emergency step - showing dialog")
            }
        if (step is EmergencyStep.Terminal){
            _showTerminalMessage.value = true
            Timber.w("Reached terminal step - showing terminal message")
        }
        if (ttsEnabled.value) {
            ttsManager.speak(step.voicePrompt)
            lastTtsCompletionTime = System.currentTimeMillis()
        }

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


    fun selectHeimlichPath(path: String) {
        _heimlichPath.value = path
        _showPathSelection.value = false
        loopCount = 0

        if (ttsEnabled.value) {
            val message = if (path == "self") {
                "Self Heimlich selected. Swipe through the steps."
            } else {
                "Helping someone selected. Call 911 first."
            }
            ttsManager.speak(message)
        }

        // Navigate to appropriate first step based on path
        val firstStep = if (path == "self") {
            "self_intro"
        } else {
            "help_call_911"
        }

        stepEngine?.goToStep(firstStep)
        Timber.i("Heimlich path selected: $path, navigating to $firstStep")
    }

    // 🔧 NEW: Check if current step is a decision point
    private fun isDecisionPoint(stepId: String): Boolean {
        return stepId == "self_check_choking" || stepId == "help_check_patient"
    }

    // 🔧 NEW: Handle loop decision
    fun onStillChoking() {
        _showLoopDialog.value = false
        loopCount++

        if (loopCount >= MAX_LOOPS) {
            // Safety: After 10 loops, show exhaustion message
            if (ttsEnabled.value) {
                ttsManager.speak("You've repeated the maneuver many times. If still choking, call 911 immediately.")
            }
            _show911Dialog.value = true
            return
        }

        // Loop back to thrust step based on path
        val loopBackStep = if (_heimlichPath.value == "self") {
            "self_thrust"
        } else {
            "help_lock_arms"
        }

        stepEngine?.goToStep(loopBackStep)
        Timber.d("Loop iteration $loopCount - returning to $loopBackStep")
    }


    // 🔧 NEW: Handle successful clearing
    fun onChokingCleared() {
        _showLoopDialog.value = false
        _showSuccessDialog.value = true

        if (ttsEnabled.value) {
            val message = if (_heimlichPath.value == "self") {
                "Good job! The obstruction has been cleared. Seek medical attention."
            } else {
                "Good job! The patient is breathing normally. Seek medical attention for them."
            }
            ttsManager.speak(message)
        }

        Timber.i("Heimlich successful - obstruction cleared")
    }

    // 🔧 NEW: Navigate to step by ID (for loops)
    private fun goToStepById(stepId: String) {
        stepEngine?.goToStep(stepId)
    }

    // 🔧 MODIFIED: Override nextStep to detect decision points
    fun nextStep() {
        if (isDialogActive()) {
            Timber.w("Blocked nextStep() - dialog is active")
            return
        }

        val currentStep = _currentStep.value

        // Check if we're at a Heimlich decision point (popup)
        if (currentStep != null && emergencyId == "emergency_heimlich") {
            if (currentStep is EmergencyStep.Popup) {
                // Don't auto-advance on popups - wait for user input
                _showLoopDialog.value = true
                return
            }
        }

        Timber.d("Manual next step triggered")
        val success = stepEngine?.nextStep() ?: false
        if (!success) {
            Timber.d("Cannot advance - at terminal or waiting for input")
        }
    }

    // 🔧 NEW: Complete Heimlich session
    fun completeHeimlichSession() {
        _showSuccessDialog.value = false
        stopListening()
        // Navigate back handled by screen
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

//    fun nextStep() {
//        if (isDialogActive()) {
//            Timber.w("Blocked nextStep() - dialog is active")
//            return
//        }
//
//        Timber.d("Manual next step triggered")
//        val success = stepEngine?.nextStep() ?: false
//        if (!success) {
//            Timber.d("Cannot advance - at terminal or waiting for input")
//        }
//    }

    fun previousStep() {
        Timber.d("Manual previous step triggered")
        stepEngine?.previousStep()
    }

    fun repeatStep() {
        val currentStep = _currentStep.value
        if (currentStep != null) {
            // 🔧 UPDATED: Only speak if TTS enabled
            if (ttsEnabled.value) {
                ttsManager.speak(currentStep.voicePrompt)
            }
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

    fun dismissTerminalPopup() {
        _showTerminalMessage.value = false
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

    // 🔧 NEW: Toggle TTS on/off
    fun toggleTts() {
        viewModelScope.launch {
            val newValue = !ttsEnabled.value
            preferencesManager.setTtsEnabled(newValue)

            if (!newValue) {
                ttsManager.stop()
                audioSessionManager.resumeAfterTts()
                Timber.i("🔇 TTS disabled by user (emergency mode)")
            } else {
                Timber.i("🔊 TTS enabled by user (emergency mode)")
                // Speak current step if enabled
                _currentStep.value?.let { step ->
                    ttsManager.speak(step.voicePrompt)
                }
            }
        }
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

// UI State Classes
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