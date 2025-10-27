package com.voxaid.core.content.model

import com.google.gson.annotations.SerializedName

/**
 * Data model for a single instruction step.
 * Contains all information needed to display and speak a step.
 */
data class Step(
    @SerializedName("step_number")
    val stepNumber: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("detailed_instructions")
    val detailedInstructions: List<String> = emptyList(),

    @SerializedName("voice_prompt")
    val voicePrompt: String,

    @SerializedName("animation_resource")
    val animationResource: String? = null, // Lottie JSON filename or URL

    @SerializedName("image_resource")
    val imageResource: String? = null, // Alternative to animation

    @SerializedName("duration_seconds")
    val durationSeconds: Int? = null, // Auto-advance timing for emergency mode

    @SerializedName("critical_warning")
    val criticalWarning: String? = null,

    @SerializedName("voice_commands")
    val voiceCommands: List<VoiceCommand> = emptyList()
)

/**
 * Voice command that can trigger actions during this step.
 */
data class VoiceCommand(
    @SerializedName("command")
    val command: String, // e.g., "next", "repeat", "back"

    @SerializedName("alternatives")
    val alternatives: List<String> = emptyList(), // Alternative phrases

    @SerializedName("action")
    val action: String // "next_step", "repeat_step", "previous_step", etc.
)

/**
 * Validation extension to ensure step data integrity.
 */
fun Step.isValid(): Boolean {
    return stepNumber > 0 &&
            title.isNotBlank() &&
            description.isNotBlank() &&
            voicePrompt.isNotBlank()
}