package com.voxaid.core.content.model

import com.google.gson.annotations.SerializedName

/**
 * Emergency-specific step types with branching, timing, and voice triggers.
 * Supports complex flows like CPR emergency mode.
 */
sealed class EmergencyStep {
    abstract val stepId: String
    abstract val title: String
    abstract val description: String
    abstract val voicePrompt: String
    abstract val animationResource: String?

    /**
     * Standard instruction step (like learning mode).
     */
    data class Instruction(
        override val stepId: String,
        override val title: String,
        override val description: String,
        override val voicePrompt: String,
        override val animationResource: String?,
        val durationSeconds: Int? = null,
        val criticalWarning: String? = null
    ) : EmergencyStep()

    /**
     * Voice-triggered step - waits for specific keyword.
     * Example: "Say 'Safe' to continue"
     */
    data class VoiceTrigger(
        override val stepId: String,
        override val title: String,
        override val description: String,
        override val voicePrompt: String,
        override val animationResource: String?,
        val expectedKeywords: List<String>, // e.g., ["safe", "ready", "yes", "no"]
        val timeoutSeconds: Int? = null // Optional auto-advance
    ) : EmergencyStep()

    /**
     * Popup with yes/no branching.
     * Example: "Is there an AED?"
     */
    data class Popup(
        override val stepId: String,
        override val title: String,
        override val description: String,
        override val voicePrompt: String,
        override val animationResource: String?,
        val yesNextStepId: String, // Step ID to jump to if "yes"
        val noNextStepId: String,  // Step ID to jump to if "no"
        val yesKeywords: List<String> = listOf("yes", "yeah", "yep", "affirmative"),
        val noKeywords: List<String> = listOf("no", "nope", "negative")
    ) : EmergencyStep()

    /**
     * Timed step with countdown.
     * Example: Chest compressions for 30 beats
     */
    data class Timed(
        override val stepId: String,
        override val title: String,
        override val description: String,
        override val voicePrompt: String,
        override val animationResource: String?,
        val durationSeconds: Int,
        val showMetronome: Boolean = false,
        val metronomeBpm: Int? = null,
        val countBeats: Boolean = false, // If true, count beats instead of seconds
        val targetBeats: Int? = null, // e.g., 30 compressions
        val timerEvents: List<TimerEvent> = emptyList() // Events at specific times
    ) : EmergencyStep()

    /**
     * Loop marker - returns to a previous step.
     * Example: Return to compression step after checking pulse
     */
    data class Loop(
        override val stepId: String,
        override val title: String,
        override val description: String,
        override val voicePrompt: String,
        override val animationResource: String?,
        val loopToStepId: String,
        val maxIterations: Int? = null // Optional loop limit
    ) : EmergencyStep()

    /**
     * Terminal step - ends the protocol.
     */
    data class Terminal(
        override val stepId: String,
        override val title: String,
        override val description: String,
        override val voicePrompt: String,
        override val animationResource: String?,
        val outcomeType: OutcomeType
    ) : EmergencyStep()
}

/**
 * Timer event triggered at specific time during a timed step.
 */
data class TimerEvent(
    @SerializedName("trigger_at_seconds")
    val triggerAtSeconds: Int, // e.g., 105 for 1:45

    @SerializedName("event_type")
    val eventType: TimerEventType,

    @SerializedName("popup_message")
    val popupMessage: String? = null,

    @SerializedName("yes_action")
    val yesAction: String? = null, // "continue" or "switch"

    @SerializedName("no_action")
    val noAction: String? = null
)

enum class TimerEventType {
    @SerializedName("popup")
    POPUP, // Show a popup dialog

    @SerializedName("notification")
    NOTIFICATION, // Show a snackbar/toast

    @SerializedName("step_transition")
    STEP_TRANSITION // Auto-advance to next step
}

enum class OutcomeType {
    @SerializedName("success")
    SUCCESS,

    @SerializedName("abort")
    ABORT,

    @SerializedName("failure")
    FAILURE,

    @SerializedName("external_help")
    EXTERNAL_HELP // Waiting for EMS
}

/**
 * Voice command mapping for emergency mode.
 */
data class EmergencyVoiceCommand(
    @SerializedName("keyword")
    val keyword: String,

    @SerializedName("alternatives")
    val alternatives: List<String> = emptyList(),

    @SerializedName("action")
    val action: String // "yes", "no", "safe", "continue", etc.
)