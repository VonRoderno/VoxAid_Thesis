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
 * State machine engine for emergency protocol execution.
 * Handles step navigation, branching, timing, and voice triggers.
 *
 * Features:
 * - Step-by-step navigation with branching logic
 * - Timer-based events and auto-transitions
 * - Voice keyword detection
 * - Loop tracking with iteration limits
 * - Terminal state detection
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
        Timber.Forest.d("EmergencyStepEngine initialized with protocol: ${protocol.name}")
        updateCurrentStep(protocol.initialStepId)
    }

    /**
     * Navigates to a specific step by ID.
     */
    fun goToStep(stepId: String) {
        if (stepId !in steps) {
            Timber.Forest.e("Step not found: $stepId")
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

    fun previousStep(): Boolean {
        if (stepHistory.isEmpty()) {
            Timber.Forest.w("No previous step available")
            return false
        }

        val previousId = stepHistory.removeAt(stepHistory.lastIndex) // pop last visited
        if (previousId in steps) {
            Timber.Forest.d("Navigating back to: $previousId")
            updateCurrentStep(previousId)
            return true
        }

        Timber.Forest.e("Previous step not found: $previousId")
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
                } else {
                    return false
                }
//        return when (current) {
//            is EmergencyStep.Instruction -> {
//                val nextId = getNextStepId(current.stepId)
//                if (nextId != null) {
//                    goToStep(nextId)
//                    true
//                } else {
//                    false
//                }
//            }
//
//            is EmergencyStep.VoiceTrigger -> {
//                // Wait for voice input - don't auto-advance
//                false
//            }
//
//            is EmergencyStep.Popup -> {
//                // Wait for user choice - don't auto-advance
//                false
//            }
//
//            is EmergencyStep.Timed -> {
//                val nextId = getNextStepId(current.stepId)
//                if (nextId != null) {
//                    goToStep(nextId)
//                    true
//                } else {
//                    false
//                }
//            }
//
//            is EmergencyStep.Loop -> {
//                handleLoop(current)
//            }
//
//            is EmergencyStep.Terminal -> {
//                false
//            }
        }

    /**
     * Handles voice keyword detection.
     * Returns true if keyword matched and navigation occurred.
     */
    /**
     * Handles voice keyword detection.
     * Returns true if keyword matched and navigation occurred.
     */
    fun handleVoiceKeyword(keyword: String): Boolean {
        val current = _currentStep.value ?: return false
        val normalizedKeyword = keyword.lowercase().trim()

        Timber.d("Attempting to match keyword: '$normalizedKeyword' against step: ${current.title}")

        return when (current) {
            is EmergencyStep.VoiceTrigger -> {
                // Check if keyword matches any expected keyword
                val matched = current.expectedKeywords.any { expected ->
                    val normalizedExpected = expected.lowercase().trim()
                    val isMatch = normalizedExpected == normalizedKeyword

                    if (isMatch) {
                        Timber.i("✓ Keyword match: '$normalizedKeyword' == '$normalizedExpected'")
                    } else {
                        Timber.v("✗ No match: '$normalizedKeyword' != '$normalizedExpected'")
                    }

                    isMatch
                }

                if (matched) {
                    Timber.i("Voice trigger matched: $normalizedKeyword → advancing to next step")
                    val nextId = getNextStepId(current.stepId)
                    if (nextId != null) {
                        goToStep(nextId)
                        true
                    } else {
                        Timber.w("Voice trigger matched but no next step defined")
                        false
                    }
                } else {
                    Timber.w("Keyword '$normalizedKeyword' not in expected list: ${current.expectedKeywords}")
                    false
                }
            }

            is EmergencyStep.Popup -> {
                // Check YES keywords
                val isYes = current.yesKeywords.any {
                    it.lowercase().trim() == normalizedKeyword
                }

                // Check NO keywords
                val isNo = current.noKeywords.any {
                    it.lowercase().trim() == normalizedKeyword
                }

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
                        Timber.w("Keyword '$normalizedKeyword' not in YES${current.yesKeywords} or NO${current.noKeywords}")
                        false
                    }
                }
            }

            else -> {
                Timber.d("Current step (${current.javaClass.simpleName}) doesn't handle voice keywords")
                false
            }
        }
    }

    /**
     * Gets the next step ID from protocol data.
     * Made public so ViewModel can use it.
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
                Timber.Forest.d("Target beats reached: $count/${current.targetBeats}")
                // Auto-advance after reaching target beats
                nextStep()
            }
        }
    }

    /**
     * Resets timer and beat count (for loops).
     */
    fun resetTimers() {
        _elapsedTime.value = 0
        _beatCount.value = 0
        triggeredEvents.clear()
    }

    /**
     * Gets the current step as a specific type (for type-safe access).
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
     * Private helper to update current step.
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

        Timber.Forest.d("Navigated to step: $stepId (${steps[stepId]?.title})")
    }

    /**
     * Handles loop logic with iteration tracking.
     */
    private fun handleLoop(loop: EmergencyStep.Loop): Boolean {
        val iterations = loopIterations.getOrPut(loop.stepId) { 0 }

        if (loop.maxIterations != null && iterations >= loop.maxIterations!!) {
            Timber.Forest.d("Loop max iterations reached for ${loop.stepId}")
            return nextStep() // Exit loop
        }

        loopIterations[loop.stepId] = iterations + 1
        Timber.Forest.d("Loop iteration ${iterations + 1} for ${loop.stepId}")

        goToStep(loop.loopToStepId)
        return true
    }

    /**
     * Handles timer events (popups, notifications, transitions).
     */
    private fun handleTimerEvent(event: TimerEvent) {
        Timber.Forest.d("Timer event triggered at ${event.triggerAtSeconds}s: ${event.eventType}")

        when (event.eventType) {
            TimerEventType.POPUP -> {
                // UI will handle showing popup - just log for now
                Timber.Forest.i("Show popup: ${event.popupMessage}")
            }

            TimerEventType.NOTIFICATION -> {
                // Show snackbar/toast
                Timber.Forest.i("Show notification: ${event.popupMessage}")
            }

            TimerEventType.STEP_TRANSITION -> {
                // Auto-advance
                nextStep()
            }
        }
    }
}