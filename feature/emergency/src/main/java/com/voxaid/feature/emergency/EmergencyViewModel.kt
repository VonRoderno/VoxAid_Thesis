//package com.voxaid.feature.emergency
//
//import androidx.lifecycle.SavedStateHandle
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.voxaid.core.audio.AudioSessionManager
//import com.voxaid.core.audio.model.VoiceIntent
//import com.voxaid.core.common.tts.TtsManager
//import com.voxaid.core.content.model.EmergencyStep
//import com.voxaid.core.content.model.LoopCondition
//import com.voxaid.core.content.model.TimerTrigger
//import com.voxaid.core.content.repository.EmergencyProtocolRepository
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.*
//import kotlinx.coroutines.launch
//import timber.log.Timber
//import javax.inject.Inject
//
///**
// * ViewModel for Emergency Mode screen.
// * Manages state machine execution, timers, and voice commands.
// *
// * Architecture:
// * - State machine drives step progression
// * - Timer triggers events at specific intervals
// * - Voice commands gate certain steps
// * - Popup decisions branch to different paths
// */
//@HiltViewModel
//class EmergencyViewModel @Inject constructor(
//    savedStateHandle: SavedStateHandle,
//    private val protocolRepository: EmergencyProtocolRepository,
//    private val audioSessionManager: AudioSessionManager,
//    private val ttsManager: TtsManager
//) : ViewModel() {
//
//    private val protocolId: String = savedStateHandle.get<String>("protocol") ?: "cpr"
//
//    private val _uiState = MutableStateFlow<EmergencyUiState>(EmergencyUiState.Loading)
//    val uiState: StateFlow<EmergencyUiState> = _uiState.asStateFlow()
//
//    private val _events = MutableSharedFlow<EmergencyEvent>()
//
//    private var globalTimerJob: Job? = null
//    private var compressionTimerJob: Job? = null
//
//    val audioState = audioSessionManager.audioState
//
//    init {
//        loadProtocol()
//        observeVoiceCommands()
//    }
//
//    /**
//     * Load emergency protocol and start at first step.
//     */
//    private fun loadProtocol() {
//        viewModelScope.launch {
//            protocolRepository.getEmergencyProtocol(protocolId)
//                .onSuccess { protocol ->
//                    val startStep = protocol.getStep(protocol.startStepId)
//
//                    if (startStep == null) {
//                        _uiState.value = EmergencyUiState.Error("Protocol start step not found")
//                        return@onSuccess
//                    }
//
//                    _uiState.value = EmergencyUiState.Active(
//                        protocol = protocol,
//                        currentStep = startStep
//                    )
//
//                    startGlobalTimer()
//                    handleStepEntry(startStep)
//
//                    Timber.i("Emergency protocol loaded: ${protocol.name}")
//                }
//                .onFailure { error ->
//                    _uiState.value = EmergencyUiState.Error(
//                        error.message ?: "Failed to load protocol"
//                    )
//                    Timber.e(error, "Failed to load emergency protocol")
//                }
//        }
//    }
//
//    /**
//     * Global timer for the entire protocol session.
//     * Triggers timer-based events.
//     */
//    private fun startGlobalTimer() {
//        globalTimerJob?.cancel()
//        globalTimerJob = viewModelScope.launch {
//            var elapsed = 0
//            while (true) {
//                delay(1000)
//                elapsed++
//
//                val state = _uiState.value as? EmergencyUiState.Active ?: break
//
//                _uiState.value = state.copy(elapsedSeconds = elapsed)
//                _events.emit(EmergencyEvent.TimerTick(elapsed))
//
//                // Check for timer triggers in current step
//                if (state.currentStep is EmergencyStep.TimedActionStep) {
//                    state.currentStep.timerTriggers[elapsed]?.let { trigger ->
//                        handleTimerTrigger(trigger)
//                    }
//                }
//            }
//        }
//    }
//
//    /**
//     * Handle timer-based triggers from TimedActionStep.
//     */
//    private fun handleTimerTrigger(trigger: TimerTrigger) {
//        val state = _uiState.value as? EmergencyUiState.Active ?: return
//
//        when (trigger) {
//            is TimerTrigger.ShowPopup -> {
//                _uiState.value = state.copy(
//                    activePopup = PopupState(
//                        title = trigger.title,
//                        message = trigger.message,
//                        options = trigger.options,
//                        optionActions = trigger.optionActions,
//                        voiceEnabled = true,
//                        source = PopupSource.TimerTriggered(trigger)
//                    )
//                )
//                Timber.d("Timer triggered popup: ${trigger.title}")
//            }
//
//            is TimerTrigger.NavigateToStep -> {
//                navigateToStep(trigger.stepId)
//                Timber.d("Timer triggered navigation to: ${trigger.stepId}")
//            }
//
//            is TimerTrigger.ShowMessage -> {
//                // TODO: Implement toast/snackbar for messages
//                Timber.d("Timer message: ${trigger.message}")
//            }
//        }
//    }
//
//    /**
//     * Handle entering a new step.
//     * Starts timers, speaks prompts, enables voice listening.
//     */
//    private fun handleStepEntry(step: EmergencyStep) {
//        val state = _uiState.value as? EmergencyUiState.Active ?: return
//
//        // Speak voice prompt
//        when (step) {
//            is EmergencyStep.InstructionStep -> {
//                ttsManager.speak(step.voicePrompt)
//
//                // Auto-advance if duration specified
//                step.durationSeconds?.let { duration ->
//                    viewModelScope.launch {
//                        delay(duration * 1000L)
//                        if (_uiState.value is EmergencyUiState.Active) {
//                            val currentState = _uiState.value as EmergencyUiState.Active
//                            if (currentState.currentStep.stepId == step.stepId) {
//                                step.nextStepId?.let { navigateToStep(it) }
//                            }
//                        }
//                    }
//                }
//            }
//
//            is EmergencyStep.VoicePromptStep -> {
//                ttsManager.speak(step.voicePrompt)
//                _uiState.value = state.copy(voiceListening = true)
//
//                // Handle timeout if specified
//                step.timeoutSeconds?.let { timeout ->
//                    viewModelScope.launch {
//                        delay(timeout * 1000L)
//                        if (_uiState.value is EmergencyUiState.Active) {
//                            val currentState = _uiState.value as EmergencyUiState.Active
//                            if (currentState.currentStep.stepId == step.stepId) {
//                                // Timeout occurred, navigate to timeout step or next step
//                                val targetStep = step.timeoutStepId ?: step.nextStepId
//                                targetStep?.let { navigateToStep(it) }
//                            }
//                        }
//                    }
//                }
//            }
//
//            is EmergencyStep.PopupDecisionStep -> {
//                _uiState.value = state.copy(
//                    activePopup = PopupState(
//                        title = step.title,
//                        message = step.message,
//                        options = step.options,
//                        optionActions = step.optionActions,
//                        voiceEnabled = step.voiceEnabled,
//                        source = PopupSource.StepBased
//                    )
//                )
//            }
//
//            is EmergencyStep.TimedActionStep -> {
//                ttsManager.speak(step.voicePrompt)
//                startCompressionTimer(step)
//
//                // Start metronome
//                _uiState.value = state.copy(
//                    isMetronomeActive = true,
//                    compressionCount = 0
//                )
//            }
//
//            is EmergencyStep.LoopControlStep -> {
//                handleLoopControl(step)
//            }
//
//            is EmergencyStep.CompletionStep -> {
//                ttsManager.speak("${step.title}. ${step.message}")
//                stopAllTimers()
//            }
//        }
//
//        Timber.d("Entered step: ${step.stepId}")
//    }
//
//    /**
//     * Timer for compression counting.
//     */
//    private fun startCompressionTimer(step: EmergencyStep.TimedActionStep) {
//        compressionTimerJob?.cancel()
//        compressionTimerJob = viewModelScope.launch {
//            val intervalMs = (60000.0 / step.metronomeBpm).toLong()
//            var count = 0
//
//            while (count < step.targetCount) {
//                delay(intervalMs)
//                count++
//
//                val state = _uiState.value as? EmergencyUiState.Active ?: break
//                _uiState.value = state.copy(compressionCount = count)
//                _events.emit(EmergencyEvent.CompressionCountIncremented(count))
//
//                Timber.v("Compression count: $count")
//            }
//
//            // Target reached, move to next step
//            step.nextStepId?.let { navigateToStep(it) }
//        }
//    }
//
//    /**
//     * Handle loop control logic.
//     */
//    private fun handleLoopControl(step: EmergencyStep.LoopControlStep) {
//        val state = _uiState.value as? EmergencyUiState.Active ?: return
//
//        val shouldContinueLoop = when (step.condition) {
//            is LoopCondition.Always -> true
//
//            is LoopCondition.TimeLimit -> {
//                state.elapsedSeconds < (step.condition as LoopCondition.TimeLimit).maxSeconds
//            }
//
//            is LoopCondition.UserConfirmation -> {
//                // Show popup to ask user
//                _uiState.value = state.copy(
//                    activePopup = PopupState(
//                        title = "Continue?",
//                        message = (step.condition as LoopCondition.UserConfirmation).promptMessage,
//                        options = listOf("Yes, Continue", "No, Stop"),
//                        optionActions = mapOf(
//                            "Yes, Continue" to step.loopStartStepId,
//                            "No, Stop" to (step.nextStepId ?: "")
//                        ),
//                        voiceEnabled = false,
//                        source = PopupSource.StepBased
//                    )
//                )
//                return // Popup will handle navigation
//            }
//        }
//
//        if (shouldContinueLoop) {
//            // Increment loop counter
//            _uiState.value = state.copy(loopIteration = state.loopIteration + 1)
//            navigateToStep(step.loopStartStepId)
//            Timber.d("Loop continued, iteration: ${state.loopIteration + 1}")
//        } else {
//            // Exit loop
//            step.nextStepId?.let { navigateToStep(it) }
//            Timber.d("Loop exited")
//        }
//    }
//
//    /**
//     * Navigate to a specific step by ID.
//     */
//    fun navigateToStep(stepId: String) {
//        val state = _uiState.value as? EmergencyUiState.Active ?: return
//        val nextStep = state.protocol.getStep(stepId)
//
//        if (nextStep == null) {
//            Timber.e("Step not found: $stepId")
//            return
//        }
//
//        // Add current step to history for back navigation
//        val newHistory = state.stepHistory + state.currentStep.stepId
//
//        // Stop compression timer if leaving a timed action step
//        if (state.currentStep is EmergencyStep.TimedActionStep) {
//            compressionTimerJob?.cancel()
//            _uiState.value = state.copy(isMetronomeActive = false)
//        }
//
//        _uiState.value = state.copy(
//            currentStep = nextStep,
//            stepHistory = newHistory,
//            activePopup = null, // Clear any active popup
//            voiceListening = false,
//            compressionCount = 0
//        )
//
//        handleStepEntry(nextStep)
//        Timber.i("Navigated to step: $stepId")
//    }
//
//    /**
//     * Handle popup option selection.
//     */
//    fun onPopupOptionSelected(option: String) {
//        val state = _uiState.value as? EmergencyUiState.Active ?: return
//        val popup = state.activePopup ?: return
//
//        val targetStepId = popup.optionActions[option]
//        if (targetStepId.isNullOrEmpty()) {
//            Timber.w("No action defined for option: $option")
//            return
//        }
//
//        Timber.d("Popup option selected: $option -> $targetStepId")
//        navigateToStep(targetStepId)
//    }
//
//    /**
//     * Manual step advancement (for non-auto-advancing instruction steps).
//     */
//    fun advanceStep() {
//        val state = _uiState.value as? EmergencyUiState.Active ?: return
//        state.currentStep.nextStepId?.let { navigateToStep(it) }
//    }
//
//    /**
//     * Observe voice commands from ASR.
//     */
//    private fun observeVoiceCommands() {
//        viewModelScope.launch {
//            audioSessionManager.recognizedIntents.collect { intent ->
//                handleVoiceIntent(intent)
//            }
//        }
//    }
//
//    /**
//     * Handle recognized voice intents.
//     */
//    private fun handleVoiceIntent(intent: VoiceIntent) {
//        val state = _uiState.value as? EmergencyUiState.Active ?: return
//
//        Timber.d("Voice intent received: $intent")
//
//        when (state.currentStep) {
//            is EmergencyStep.VoicePromptStep -> {
//                val step = state.currentStep as EmergencyStep.VoicePromptStep
//                val command = when (intent) {
//                    is VoiceIntent.Unknown -> intent.rawText.lowercase()
//                    else -> intent.toString().lowercase()
//                }
//
//                // Check if command matches expected commands
//                if (step.expectedCommands.any { it in command }) {
//                    Timber.i("Voice command matched: $command")
//                    _uiState.value = state.copy(voiceListening = false)
//                    step.nextStepId?.let { navigateToStep(it) }
//                }
//            }
//
//            is EmergencyStep.PopupDecisionStep -> {
//                val popup = state.activePopup
//                if (popup != null && popup.voiceEnabled) {
//                    val command = when (intent) {
//                        is VoiceIntent.Unknown -> intent.rawText.lowercase()
//                        else -> intent.toString().lowercase()
//                    }
//
//                    // Match voice command to popup options
//                    popup.options.firstOrNull { option ->
//                        option.lowercase() in command
//                    }?.let { matchedOption ->
//                        Timber.i("Voice matched popup option: $matchedOption")
//                        onPopupOptionSelected(matchedOption)
//                    }
//                }
//            }
//
//            else -> {
//                // Handle general navigation commands
//                when (intent) {
//                    is VoiceIntent.NextStep -> advanceStep()
//                    is VoiceIntent.RepeatStep -> repeatCurrentStep()
//                    is VoiceIntent.Call911 -> {
//                        // Show 911 dialog
//                        Timber.w("Voice command: Call 911")
//                    }
//                    else -> Timber.d("Unhandled voice intent in current step")
//                }
//            }
//        }
//    }
//
//    /**
//     * Repeat current step (re-speak voice prompt).
//     */
//    fun repeatCurrentStep() {
//        val state = _uiState.value as? EmergencyUiState.Active ?: return
//
//        when (val step = state.currentStep) {
//            is EmergencyStep.InstructionStep -> ttsManager.speak(step.voicePrompt)
//            is EmergencyStep.VoicePromptStep -> ttsManager.speak(step.voicePrompt)
//            is EmergencyStep.TimedActionStep -> ttsManager.speak(step.voicePrompt)
//            else -> {}
//        }
//
//        Timber.d("Repeated current step")
//    }
//
//    /**
//     * Start audio session (ASR + TTS).
//     */
//    fun startListening() {
//        viewModelScope.launch {
//            audioSessionManager.initialize()
//            audioSessionManager.startSession()
//            Timber.d("Audio session started for emergency mode")
//        }
//    }
//
//    /**
//     * Stop audio session.
//     */
//    fun stopListening() {
//        audioSessionManager.stopSession()
//        ttsManager.stop()
//        stopAllTimers()
//        Timber.d("Audio session stopped")
//    }
//
//    /**
//     * Stop all running timers.
//     */
//    private fun stopAllTimers() {
//        globalTimerJob?.cancel()
//        compressionTimerJob?.cancel()
//    }
//
//    /**
//     * Cleanup on ViewModel destruction.
//     */
//    override fun onCleared() {
//        super.onCleared()
//        stopListening()
//    }
//}