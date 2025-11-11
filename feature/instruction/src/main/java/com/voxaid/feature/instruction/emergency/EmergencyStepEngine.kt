package com.voxaid.feature.instruction.emergency

import com.voxaid.core.content.model.EmergencyProtocol
import com.voxaid.core.content.model.EmergencyStep
import com.voxaid.core.content.model.TimerEvent
import com.voxaid.core.content.model.TimerEventType
import com.voxaid.core.content.model.toEmergencyStep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * State machine engine for emergency protocol execution with swipe navigation support.
 *
 * Features:
 * - Step-by-step navigation with branching logic
 * - Swipeable step sequence for HorizontalPager
 * - Timer-based events and auto-transitions
 * - Voice keyword detection
 * - Loop tracking with iteration limits
 * - Terminal state detection
 * - Index-based navigation for UI sync
 *
 * Swipe Navigation:
 * - Builds linear step sequence for pager
 * - Disables swipe for voice-triggered steps
 * - Maintains index synchronization
 * - Supports both swipe and programmatic navigation
 */
class EmergencyStepEngine(
    private val protocol: EmergencyProtocol
) {
    private val steps = protocol.steps.mapValues { (id, data) ->
        data.toEmergencyStep(id)
    }

    private val _currentStepId = MutableStateFlow(protocol.initialStepId)
    val currentStepId: StateFlow<String> = _currentStepId.asStateFlow()

    val _currentStep = MutableStateFlow<EmergencyStep?>(steps[protocol.initialStepId])
    val currentStep: StateFlow<EmergencyStep?> = _currentStep.asStateFlow()

    private val _elapsedTime = MutableStateFlow(0)
    val elapsedTime: StateFlow<Int> = _elapsedTime.asStateFlow()

    private val _beatCount = MutableStateFlow(0)
    val beatCount: StateFlow<Int> = _beatCount.asStateFlow()

    private val _isAtTerminalStep = MutableStateFlow(false)
    val isAtTerminalStep: StateFlow<Boolean> = _isAtTerminalStep.asStateFlow()

    // Track loop iterations to prevent infinite loops
    private val loopIterations = mutableMapOf<String, Int>()

    // Track triggered timer events to prevent duplicates
    private val triggeredEvents = mutableSetOf<String>()

    // Step history to support back navigation
    private val stepHistory = mutableListOf<String>()

    init {
        Timber.d("EmergencyStepEngine initialized with protocol: ${protocol.name}")
        updateCurrentStep(protocol.initialStepId)
    }

    /**
     * Navigates to a specific step by ID (programmatic navigation).
     */
    fun goToStep(stepId: String) {
        if (stepId !in steps) {
            Timber.e("Step not found: $stepId")
            return
        }

        // Record history (avoid duplicates when first loading)
        _currentStep.value?.stepId?.let { currentId ->
            if (currentId != stepId) {
                stepHistory.add(currentId)
            }
        }

        updateCurrentStep(stepId)
    }

    /**
     * Navigate to previous step in history.
     */
    fun previousStep(): Boolean {
        if (stepHistory.isEmpty()) {
            Timber.w("No previous step available")
            return false
        }

        val previousId = stepHistory.removeAt(stepHistory.lastIndex)
        if (previousId in steps) {
            Timber.d("Navigating back to: $previousId")
            updateCurrentStep(previousId)
            return true
        }

        Timber.e("Previous step not found: $previousId")
        return false
    }

    /**
     * Advances to the next step based on current step type.
     * Returns true if navigation succeeded, false if at terminal step.
     */
    fun nextStep(): Boolean {
        val current = _currentStep.value ?: return false
        val nextId = getNextStepId(current.stepId)

        if (nextId != null) {
            goToStep(nextId)
            return true
        }

        return false
    }

    /**
     * Handles voice keyword detection with special routing for tap_shout step.
     * Returns true if keyword matched and navigation occurred.
     */
    fun handleVoiceKeyword(keyword: String): Boolean {
        val current = _currentStep.value ?: return false
        val normalizedKeyword = keyword.lowercase().trim()

        Timber.d("Attempting to match keyword: '$normalizedKeyword' against step: ${current.title}")

        // Special handling for tap_shout step
        if (current.stepId == "tap_shout") {
            return when {
                normalizedKeyword in listOf("yes", "responsive", "conscious", "awake", "responding") -> {
                    Timber.i("✓ Patient RESPONSIVE - navigating to patient_recovered")
                    goToStep("patient_recovered")
                    true
                }
                normalizedKeyword in listOf("no", "whoa", "unresponsive", "unconscious", "not responding", "no response") -> {
                    Timber.i("✓ Patient UNRESPONSIVE - returning to chest_compressions")
                    goToStep("chest_compressions")
                    true
                }
                else -> {
                    Timber.w("Keyword '$normalizedKeyword' not recognized for tap_shout step")
                    false
                }
            }
        }

        // Standard voice trigger handling
        return when (current) {
            is EmergencyStep.VoiceTrigger -> {
                val matched = current.expectedKeywords.any { expected ->
                    expected.lowercase().trim() == normalizedKeyword
                }

                if (matched) {
                    Timber.i("Voice trigger matched: $normalizedKeyword → advancing to next step")
                    nextStep()
                } else {
                    Timber.w("Keyword '$normalizedKeyword' not in expected list: ${current.expectedKeywords}")
                    false
                }
            }

            is EmergencyStep.Popup -> {
                val isYes = current.yesKeywords.any { it.lowercase().trim() == normalizedKeyword }
                val isNo = current.noKeywords.any { it.lowercase().trim() == normalizedKeyword }

                when {
                    isYes -> {
                        Timber.i("Popup YES matched: $normalizedKeyword")
                        goToStep(current.yesNextStepId)
                        true
                    }
                    isNo -> {
                        Timber.i("Popup NO matched: $normalizedKeyword")
                        goToStep(current.noNextStepId)
                        true
                    }
                    else -> {
                        Timber.w("Keyword '$normalizedKeyword' not in YES or NO keywords")
                        false
                    }
                }
            }

            else -> {
                Timber.d("Current step doesn't handle voice keywords")
                false
            }
        }
    }

    /**
     * Gets the next step ID from protocol data.
     */
    fun getNextStepId(currentStepId: String): String? {
        val rawData = protocol.steps[currentStepId]
        return rawData?.nextStepId
    }

    /**
     * Handles popup button selection (yes/no).
     */
    fun handlePopupSelection(isYes: Boolean) {
        val current = _currentStep.value
        if (current !is EmergencyStep.Popup) return

        val nextId = if (isYes) current.yesNextStepId else current.noNextStepId
        goToStep(nextId)
    }

    /**
     * Updates elapsed time for timed steps.
     * Checks for timer events and auto-transitions.
     */
    fun updateElapsedTime(seconds: Int) {
        _elapsedTime.value = seconds

        val current = _currentStep.value
        if (current is EmergencyStep.Timed) {
            // Check for timer events
            current.timerEvents.forEach { event ->
                val eventKey = "${current.stepId}_${event.triggerAtSeconds}"

                if (seconds >= event.triggerAtSeconds && eventKey !in triggeredEvents) {
                    triggeredEvents.add(eventKey)
                    handleTimerEvent(event)
                }
            }

            // Auto-advance when duration reached
            if (seconds >= current.durationSeconds) {
                nextStep()
            }
        }
    }

    /**
     * Updates beat count for compression tracking.
     */
    fun updateBeatCount(count: Int) {
        _beatCount.value = count

        val current = _currentStep.value
        if (current is EmergencyStep.Timed && current.countBeats) {
            if (current.targetBeats != null && count >= current.targetBeats!!) {
                Timber.d("Target beats reached: $count/${current.targetBeats}")
            }
        }
    }

    /**
     * Resets timer and beat count.
     */
    fun resetTimers() {
        _elapsedTime.value = 0
        _beatCount.value = 0
        triggeredEvents.clear()
    }

    /**
     * Gets the current step as a specific type.
     */
    inline fun <reified T : EmergencyStep> getCurrentStepAs(): T? {
        return _currentStep.value as? T
    }

    /**
     * Checks if current step requires voice input.
     */
    fun isWaitingForVoiceInput(): Boolean {
        return _currentStep.value is EmergencyStep.VoiceTrigger ||
                _currentStep.value is EmergencyStep.Popup
    }

    /**
     * Checks if current step is timed.
     */
    fun isTimedStep(): Boolean {
        return _currentStep.value is EmergencyStep.Timed
    }

    /**
     * Updates current step state.
     */
    private fun updateCurrentStep(stepId: String) {
        _currentStepId.value = stepId
        _currentStep.value = steps[stepId]

        // Check if terminal
        _isAtTerminalStep.value = steps[stepId] is EmergencyStep.Terminal

        // Reset timers for new step
        if (steps[stepId] is EmergencyStep.Timed) {
            resetTimers()
        }

        Timber.d("Navigated to step: $stepId (${steps[stepId]?.title})")
    }

    /**
     * Handles timer events.
     */
    private fun handleTimerEvent(event: TimerEvent) {
        Timber.d("Timer event triggered at ${event.triggerAtSeconds}s: ${event.eventType}")

        when (event.eventType) {
            TimerEventType.POPUP -> {
                Timber.i("Show popup: ${event.popupMessage}")
            }
            TimerEventType.NOTIFICATION -> {
                Timber.i("Show notification: ${event.popupMessage}")
            }
            TimerEventType.STEP_TRANSITION -> {
                nextStep()
            }
        }
    }
}