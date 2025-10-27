package com.voxaid.core.content.model

import com.google.gson.annotations.SerializedName

/**
 * Data model for a first aid protocol.
 * Contains metadata and sequence of instruction steps.
 */
data class Protocol(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("category")
    val category: String,

    @SerializedName("estimated_duration_minutes")
    val estimatedDurationMinutes: Int,

    @SerializedName("warning")
    val warning: String? = null,

    @SerializedName("steps")
    val steps: List<Step>,

    @SerializedName("emergency_notes")
    val emergencyNotes: List<String> = emptyList(),

    @SerializedName("metronome_bpm")
    val metronomeBpm: Int? = null // For CPR timing
)

/**
 * Validation extension to ensure protocol data integrity.
 */
fun Protocol.isValid(): Boolean {
    return id.isNotBlank() &&
            name.isNotBlank() &&
            steps.isNotEmpty() &&
            steps.all { it.isValid() }
}