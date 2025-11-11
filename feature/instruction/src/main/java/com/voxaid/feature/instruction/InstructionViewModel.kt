package com.voxaid.feature.instruction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voxaid.core.audio.AudioSessionManager
import com.voxaid.core.audio.model.VoiceIntent
import com.voxaid.core.common.datastore.EmergencyUnlockManager
import com.voxaid.core.common.datastore.PreferencesManager
import com.voxaid.core.common.datastore.ProtocolCompletionManager
import com.voxaid.core.common.model.UnlockResult
import com.voxaid.core.common.tts.TtsEvent
import com.voxaid.core.common.tts.TtsManager
import com.voxaid.core.content.model.Protocol
import com.voxaid.core.content.model.Step
import com.voxaid.core.content.repository.ProtocolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class InstructionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val protocolRepository: ProtocolRepository,
    private val ttsManager: TtsManager,
    private val audioSessionManager: AudioSessionManager,
    private val preferencesManager: PreferencesManager,
    private val completionManager: ProtocolCompletionManager,
    private val emergencyUnlockManager: EmergencyUnlockManager
) : ViewModel() {

    private val mode: String = savedStateHandle.get<String>("mode") ?: "instructional"
    private val variantId: String = savedStateHandle.get<String>("variant") ?: "cpr_1person"

    val isEmergencyMode = mode == "emergency"

    private val _uiState = MutableStateFlow<InstructionUiState>(InstructionUiState.Loading)
    val uiState: StateFlow<InstructionUiState> = _uiState.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private var protocol: Protocol? = null

    // 🔧 NEW: Expose TTS enabled state
    val ttsEnabled = preferencesManager.ttsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val autoAdvanceEnabled = MutableStateFlow(isEmergencyMode)

    val audioState = audioSessionManager.audioState

    private val _isMetronomeActive = MutableStateFlow(false)
    val isMetronomeActive: StateFlow<Boolean> = _isMetronomeActive.asStateFlow()

    val metronomeBpm: Int = 110

    private val _show911Dialog = MutableStateFlow(false)
    val show911Dialog: StateFlow<Boolean> = _show911Dialog.asStateFlow()

    private val _showCompletionDialog = MutableStateFlow(false)
    val showCompletionDialog: StateFlow<Boolean> = _showCompletionDialog.asStateFlow()

    private val _showEmergencyUnlockedDialog = MutableStateFlow(false)
    val showEmergencyUnlockedDialog: StateFlow<Boolean> = _showEmergencyUnlockedDialog.asStateFlow()

    fun dismissEmergencyUnlockedDialog() {
        _showEmergencyUnlockedDialog.value = false
    }

    init {
        loadProtocol()
        observePreferences()
        observeTtsEvents()
        observeVoiceCommands()
        observeTtsState()
        initializeAudio()
    }

    private fun initializeAudio() {
        viewModelScope.launch {
            audioSessionManager.initialize()
        }
    }

    // 🔧 UPDATED: Only pause ASR if TTS is enabled
    private fun observeTtsState() {
        viewModelScope.launch {
            combine(
                ttsManager.isSpeaking,
                ttsEnabled
            ) { isSpeaking, enabled ->
                Pair(isSpeaking, enabled)
            }.collect { (isSpeaking, enabled) ->
                // Only coordinate with ASR if TTS is actually enabled
                if (enabled) {
                    if (isSpeaking) {
                        audioSessionManager.pauseForTts()
                        Timber.d("🔇 ASR paused - TTS is speaking")
                    } else {
                        delay(500)
                        audioSessionManager.resumeAfterTts()
                        Timber.d("🔊 ASR resumed - TTS finished")
                    }
                } else {
                    // TTS disabled - ensure ASR is always active
                    Timber.d("🎤 TTS disabled - ASR stays active")
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
        // 🔧 UPDATED: Skip cooldown check if TTS is disabled
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
                        lastTtsCompletionTime = System.currentTimeMillis()
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
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = InstructionUiState.Loading


            protocolRepository.getProtocol(variantId)
                .onSuccess { loadedProtocol ->
                    protocol = loadedProtocol
                    _uiState.value = InstructionUiState.Success(
                        protocol = loadedProtocol,
                        currentStep = loadedProtocol.steps[0]
                    )

                    // 🔧 UPDATED: Only speak if TTS enabled
                    if (ttsEnabled.value) {
                        speakStep(loadedProtocol.steps[0])
                    }

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

            // 🔧 UPDATED: Only speak if TTS enabled
            if (ttsEnabled.value) {
                speakStep(nextStep)
            }

            if (!isEmergencyMode) {
                viewModelScope.launch {
                    completionManager.updateProgress(variantId, nextIndex)
                }
            }

            if (!isEmergencyMode && nextIndex == currentProtocol.steps.size - 1) {
                markProtocolAsCompleted()
            }

            Timber.d("Moved to step $nextIndex")
        }
    }

    private fun markProtocolAsCompleted() {
        viewModelScope.launch {
            when (val result = completionManager.markAsCompleted(variantId)) {
                is UnlockResult.NewlyUnlocked -> {
                    Timber.i("Protocol $variantId newly unlocked!")
                    _showCompletionDialog.value = true
                    checkEmergencyUnlock()
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

    private fun checkEmergencyUnlock() {
        viewModelScope.launch {
            val protocolCategory = when {
                variantId.contains("cpr") -> "cpr"
                variantId.contains("heimlich") -> "heimlich"
                variantId.contains("bandaging") -> "bandaging"
                else -> null
            }

            protocolCategory?.let { category ->
                val wasUnlocked = emergencyUnlockManager.checkAndMarkNewlyUnlocked(category)

                if (wasUnlocked) {
                    Timber.i("🎉 Emergency mode unlocked for $category!")
                    _showEmergencyUnlockedDialog.value = true
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

            // 🔧 UPDATED: Only speak if TTS enabled
            if (ttsEnabled.value) {
                speakStep(prevStep)
            }

            Timber.d("Moved to step $prevIndex")
        }
    }

    fun repeatStep() {
        val currentProtocol = protocol ?: return
        val currentStep = currentProtocol.steps.getOrNull(_currentStepIndex.value) ?: return

        // 🔧 UPDATED: Only speak if TTS enabled
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

            // 🔧 UPDATED: Only speak if TTS enabled
            if (ttsEnabled.value) {
                speakStep(step)
            }

            if (!isEmergencyMode) {
                viewModelScope.launch {
                    completionManager.updateProgress(variantId, stepIndex)
                }
            }

            if (!isEmergencyMode && stepIndex == currentProtocol.steps.size - 1) {
                markProtocolAsCompleted()
            }

            Timber.d("Jumped to step $stepIndex")
        }
    }

    private fun speakStep(step: Step) {
        ttsManager.speak(step.voicePrompt)
    }

    // 🔧 NEW: Toggle TTS on/off
    fun toggleTts() {
        viewModelScope.launch {
            val newValue = !ttsEnabled.value
            preferencesManager.setTtsEnabled(newValue)

            if (!newValue) {
                ttsManager.stop()
                audioSessionManager.resumeAfterTts()
                Timber.i("🔇 TTS disabled by user")
            } else {
                Timber.i("🔊 TTS enabled by user")
                // Speak current step if enabled
                val currentStep = protocol?.steps?.getOrNull(_currentStepIndex.value)
                currentStep?.let { speakStep(it) }
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

sealed class InstructionUiState {
    data object Loading : InstructionUiState()
    data class Success(
        val protocol: Protocol,
        val currentStep: Step
    ) : InstructionUiState()
    data class Error(val message: String) : InstructionUiState()
}