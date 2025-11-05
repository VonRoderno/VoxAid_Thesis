package com.voxaid.core.content.model

import com.google.gson.annotations.SerializedName

/**
 * Emergency mode protocol with step engine support.
 * Supports branching, timing, voice triggers, and loops.
 */
data class EmergencyProtocol(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("category")
    val category: String,

    @SerializedName("required_learning_variants")
    val requiredLearningVariants: List<String>, // Must complete all to unlock

    @SerializedName("warning")
    val warning: String?,

    @SerializedName("emergency_notes")
    val emergencyNotes: List<String> = emptyList(),

    @SerializedName("metronome_bpm")
    val metronomeBpm: Int? = null,

    @SerializedName("initial_step_id")
    val initialStepId: String, // Starting step

    @SerializedName("steps")
    val steps: Map<String, EmergencyStepData> // Map of stepId -> step data
)

/**
 * Raw step data from JSON (will be converted to EmergencyStep sealed class).
 */
data class EmergencyStepData(
    @SerializedName("step_type")
    val stepType: String, // "instruction", "voice_trigger", "popup", "timed", "loop", "terminal"

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("voice_prompt")
    val voicePrompt: String,

    @SerializedName("animation_resource")
    val animationResource: String? = null,

    // VoiceTrigger fields
    @SerializedName("expected_keywords")
    val expectedKeywords: List<String>? = null,

    @SerializedName("timeout_seconds")
    val timeoutSeconds: Int? = null,

    // Popup fields
    @SerializedName("yes_next_step_id")
    val yesNextStepId: String? = null,

    @SerializedName("no_next_step_id")
    val noNextStepId: String? = null,

    @SerializedName("yes_keywords")
    val yesKeywords: List<String>? = null,

    @SerializedName("no_keywords")
    val noKeywords: List<String>? = null,

    // Timed fields
    @SerializedName("duration_seconds")
    val durationSeconds: Int? = null,

    @SerializedName("show_metronome")
    val showMetronome: Boolean? = null,

    @SerializedName("metronome_bpm")
    val metronomeBpm: Int? = null,

    @SerializedName("count_beats")
    val countBeats: Boolean? = null,

    @SerializedName("target_beats")
    val targetBeats: Int? = null,

    @SerializedName("timer_events")
    val timerEvents: List<TimerEvent>? = null,

    // Loop fields
    @SerializedName("loop_to_step_id")
    val loopToStepId: String? = null,

    @SerializedName("max_iterations")
    val maxIterations: Int? = null,

    // Terminal fields
    @SerializedName("outcome_type")
    val outcomeType: String? = null,

    // Standard fields
    @SerializedName("critical_warning")
    val criticalWarning: String? = null,

    @SerializedName("next_step_id")
    val nextStepId: String? = null // Default next step (for non-branching)
)

/**
 * Extension to convert raw data to typed sealed class.
 */
fun EmergencyStepData.toEmergencyStep(stepId: String): EmergencyStep {
    return when (stepType) {
        "instruction" -> EmergencyStep.Instruction(
            stepId = stepId,
            title = title,
            description = description,
            voicePrompt = voicePrompt,
            animationResource = animationResource,
            durationSeconds = durationSeconds,
            criticalWarning = criticalWarning
        )

        "voice_trigger" -> EmergencyStep.VoiceTrigger(
            stepId = stepId,
            title = title,
            description = description,
            voicePrompt = voicePrompt,
            animationResource = animationResource,
            expectedKeywords = expectedKeywords ?: emptyList(),
            timeoutSeconds = timeoutSeconds
        )

        "popup" -> EmergencyStep.Popup(
            stepId = stepId,
            title = title,
            description = description,
            voicePrompt = voicePrompt,
            animationResource = animationResource,
            yesNextStepId = yesNextStepId ?: "",
            noNextStepId = noNextStepId ?: "",
            yesKeywords = yesKeywords ?: listOf("yes", "yeah", "yep"),
            noKeywords = noKeywords ?: listOf("no", "nope")
        )

        "timed" -> EmergencyStep.Timed(
            stepId = stepId,
            title = title,
            description = description,
            voicePrompt = voicePrompt,
            animationResource = animationResource,
            durationSeconds = durationSeconds ?: 0,
            showMetronome = showMetronome ?: false,
            metronomeBpm = metronomeBpm,
            countBeats = countBeats ?: false,
            targetBeats = targetBeats,
            timerEvents = timerEvents ?: emptyList()
        )

        "loop" -> EmergencyStep.Loop(
            stepId = stepId,
            title = title,
            description = description,
            voicePrompt = voicePrompt,
            animationResource = animationResource,
            loopToStepId = loopToStepId ?: "",
            maxIterations = maxIterations
        )

        "terminal" -> EmergencyStep.Terminal(
            stepId = stepId,
            title = title,
            description = description,
            voicePrompt = voicePrompt,
            animationResource = animationResource,
            outcomeType = when (outcomeType) {
                "success" -> OutcomeType.SUCCESS
                "abort" -> OutcomeType.ABORT
                "failure" -> OutcomeType.FAILURE
                "external_help" -> OutcomeType.EXTERNAL_HELP
                else -> OutcomeType.SUCCESS
            }
        )

        else -> throw IllegalArgumentException("Unknown step type: $stepType")
    }
}