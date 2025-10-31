package com.voxaid.feature.instruction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voxaid.core.audio.AudioSessionManager
import com.voxaid.core.audio.model.VoiceIntent
import com.voxaid.core.common.datastore.PreferencesManager
import com.voxaid.core.common.datastore.ProtocolCompletionManager
import com.voxaid.core.common.model.UnlockResult
import com.voxaid.core.common.tts.TtsEvent
import com.voxaid.core.common.tts.TtsManager
import com.voxaid.core.content.model.Protocol
import com.voxaid.core.content.model.Step
import com.voxaid.core.content.repository.ProtocolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for instruction screen.
 * Manages protocol data, step navigation, TTS, ASR, voice commands,
 * and protocol completion tracking.
 */
@HiltViewModel
class InstructionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val protocolRepository: ProtocolRepository,
    private val ttsManager: TtsManager,
    private val audioSessionManager: AudioSessionManager,
    private val preferencesManager: PreferencesManager,
    private val completionManager: ProtocolCompletionManager
) : ViewModel() {

    // Get navigation arguments from SavedStateHandle
    private val mode: String = savedStateHandle.get<String>("mode") ?: "instructional"
    private val variantId: String = savedStateHandle.get<String>("variant") ?: "cpr_1person"

    val isEmergencyMode = mode == "emergency"

    private val _uiState = MutableStateFlow<InstructionUiState>(InstructionUiState.Loading)
    val uiState: StateFlow<InstructionUiState> = _uiState.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private var protocol: Protocol? = null

    private val ttsEnabled = MutableStateFlow(true)
    private val autoAdvanceEnabled = MutableStateFlow(isEmergencyMode)

    val audioState = audioSessionManager.audioState

    // Metronome state
    private val _isMetronomeActive = MutableStateFlow(false)
    val isMetronomeActive: StateFlow<Boolean> = _isMetronomeActive.asStateFlow()

    val metronomeBpm: Int = 110 // CPR recommended rate

    // 911 dialog state
    private val _show911Dialog = MutableStateFlow(false)
    val show911Dialog: StateFlow<Boolean> = _show911Dialog.asStateFlow()

    // Completion dialog state
    private val _showCompletionDialog = MutableStateFlow(false)
    val showCompletionDialog: StateFlow<Boolean> = _showCompletionDialog.asStateFlow()

    init {
        loadProtocol()
        observePreferences()
        observeTtsEvents()
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
        Timber.d("Handling voice intent: $intent")

        when (intent) {
            is VoiceIntent.NextStep -> nextStep()
            is VoiceIntent.PreviousStep -> previousStep()
            is VoiceIntent.RepeatStep -> repeatStep()
            is VoiceIntent.GoToStep -> {
                goToStep(intent.stepNumber - 1)
            }
            is VoiceIntent.StartMetronome -> startMetronome()
            is VoiceIntent.StopMetronome -> stopMetronome()
            is VoiceIntent.Help -> {
                repeatStep()
            }
            is VoiceIntent.Call911 -> {
                _show911Dialog.value = true
                Timber.w("Voice command: Call 911 - showing dialog")
            }
            else -> {
                Timber.d("Unhandled voice intent: $intent")
            }
        }
    }

    fun dismiss911Dialog() {
        _show911Dialog.value = false
    }

    fun show911Dialog() {
        _show911Dialog.value = true
        Timber.i("Call 911 dialog shown via button tap")
    }

    fun dismissCompletionDialog() {
        _showCompletionDialog.value = false
    }

    fun startMetronome() {
        _isMetronomeActive.value = true
        Timber.d("Metronome started")
    }

    fun stopMetronome() {
        _isMetronomeActive.value = false
        Timber.d("Metronome stopped")
    }

    fun toggleMetronome() {
        _isMetronomeActive.value = !_isMetronomeActive.value
    }

    fun startListening() {
        audioSessionManager.startSession()
        Timber.d("Started voice listening")
    }

    fun stopListening() {
        audioSessionManager.stopSession()
        Timber.d("Stopped voice listening")
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesManager.ttsEnabled.collect { enabled ->
                ttsEnabled.value = enabled
                Timber.d("TTS enabled: $enabled")
            }
        }

        viewModelScope.launch {
            preferencesManager.autoAdvanceEnabled.collect { enabled ->
                autoAdvanceEnabled.value = enabled && isEmergencyMode
                Timber.d("Auto-advance enabled: $enabled")
            }
        }
    }

    private fun observeTtsEvents() {
        viewModelScope.launch {
            ttsManager.ttsEvents.collect { event ->
                when (event) {
                    is TtsEvent.Completed -> {
                        if (autoAdvanceEnabled.value && isEmergencyMode) {
                            val currentStep = protocol?.steps?.getOrNull(_currentStepIndex.value)
                            currentStep?.durationSeconds?.let { duration ->
                                delay(duration * 1000L)
                                if (_currentStepIndex.value < (protocol?.steps?.size ?: 0) - 1) {
                                    nextStep()
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun loadProtocol() {
        viewModelScope.launch {
            _uiState.value = InstructionUiState.Loading

            protocolRepository.getProtocol(variantId)
                .onSuccess { loadedProtocol ->
                    protocol = loadedProtocol
                    _uiState.value = InstructionUiState.Success(
                        protocol = loadedProtocol,
                        currentStep = loadedProtocol.steps[0]
                    )

                    // Speak first step if TTS enabled
                    if (ttsEnabled.value) {
                        speakStep(loadedProtocol.steps[0])
                    }

                    // Update progress tracking
                    if (!isEmergencyMode) {
                        completionManager.updateProgress(variantId, 0)
                    }

                    Timber.i("Protocol loaded: ${loadedProtocol.name}")
                }
                .onFailure { error ->
                    _uiState.value = InstructionUiState.Error(
                        error.message ?: "Failed to load protocol"
                    )
                    Timber.e(error, "Failed to load protocol")
                }
        }
    }

    fun nextStep() {
        val currentProtocol = protocol ?: return
        val nextIndex = _currentStepIndex.value + 1

        if (nextIndex < currentProtocol.steps.size) {
            _currentStepIndex.value = nextIndex
            val nextStep = currentProtocol.steps[nextIndex]

            _uiState.value = InstructionUiState.Success(
                protocol = currentProtocol,
                currentStep = nextStep
            )

            if (ttsEnabled.value) {
                speakStep(nextStep)
            }

            // Update progress in instructional mode
            if (!isEmergencyMode) {
                viewModelScope.launch {
                    completionManager.updateProgress(variantId, nextIndex)
                }
            }

            // Check if this is the final step in instructional mode
            if (!isEmergencyMode && nextIndex == currentProtocol.steps.size - 1) {
                markProtocolAsCompleted()
            }

            Timber.d("Moved to step $nextIndex")
        } else {
            Timber.d("Already at last step")
        }
    }

    /**
     * Marks the protocol as completed and unlocks it for emergency mode.
     * Called when user reaches the final step in instructional mode.
     */
    private fun markProtocolAsCompleted() {
        viewModelScope.launch {
            when (val result = completionManager.markAsCompleted(variantId)) {
                is UnlockResult.NewlyUnlocked -> {
                    Timber.i("Protocol $variantId newly unlocked!")
                    // Show celebration dialog
                    _showCompletionDialog.value = true
                }
                is UnlockResult.AlreadyUnlocked -> {
                    Timber.d("Protocol $variantId was already unlocked")
                }
                is UnlockResult.StillLocked -> {
                    Timber.w("Protocol $variantId still locked: ${result.reason}")
                }
            }
        }
    }

    fun previousStep() {
        val currentProtocol = protocol ?: return
        val prevIndex = _currentStepIndex.value - 1

        if (prevIndex >= 0) {
            _currentStepIndex.value = prevIndex
            val prevStep = currentProtocol.steps[prevIndex]

            _uiState.value = InstructionUiState.Success(
                protocol = currentProtocol,
                currentStep = prevStep
            )

            if (ttsEnabled.value) {
                speakStep(prevStep)
            }

            Timber.d("Moved to step $prevIndex")
        }
    }

    fun repeatStep() {
        val currentProtocol = protocol ?: return
        val currentStep = currentProtocol.steps.getOrNull(_currentStepIndex.value) ?: return

        if (ttsEnabled.value) {
            speakStep(currentStep)
        }

        Timber.d("Repeating step ${_currentStepIndex.value}")
    }

    fun goToStep(stepIndex: Int) {
        val currentProtocol = protocol ?: return

        if (stepIndex in currentProtocol.steps.indices) {
            _currentStepIndex.value = stepIndex
            val step = currentProtocol.steps[stepIndex]

            _uiState.value = InstructionUiState.Success(
                protocol = currentProtocol,
                currentStep = step
            )

            if (ttsEnabled.value) {
                speakStep(step)
            }

            // Update progress
            if (!isEmergencyMode) {
                viewModelScope.launch {
                    completionManager.updateProgress(variantId, stepIndex)
                }
            }

            // Check if jumped to final step
            if (!isEmergencyMode && stepIndex == currentProtocol.steps.size - 1) {
                markProtocolAsCompleted()
            }

            Timber.d("Jumped to step $stepIndex")
        }
    }

    private fun speakStep(step: Step) {
        ttsManager.speak(step.voicePrompt)
    }

    fun toggleTts() {
        viewModelScope.launch {
            val newValue = !ttsEnabled.value
            preferencesManager.setTtsEnabled(newValue)

            if (!newValue) {
                ttsManager.stop()
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
        stopMetronome()
    }
}

/**
 * UI state for instruction screen.
 */
sealed class InstructionUiState {
    data object Loading : InstructionUiState()
    data class Success(
        val protocol: Protocol,
        val currentStep: Step
    ) : InstructionUiState()
    data class Error(val message: String) : InstructionUiState()
}