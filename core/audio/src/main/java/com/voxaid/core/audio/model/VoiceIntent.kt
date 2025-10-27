package com.voxaid.core.audio.model

/**
* Recognized voice intents from ASR.
* Maps recognized speech to actionable commands.
*/
sealed class VoiceIntent {
    data object NextStep : VoiceIntent()
    data object PreviousStep : VoiceIntent()
    data object RepeatStep : VoiceIntent()
    data object StartMetronome : VoiceIntent()
    data object StopMetronome : VoiceIntent()
    data object Help : VoiceIntent()
    data object Call911 : VoiceIntent()

    data class GoToStep(val stepNumber: Int) : VoiceIntent()
    data class Unknown(val rawText: String) : VoiceIntent()

    companion object {
        /**
         * Parses raw recognized text into a VoiceIntent.
         */
        fun fromText(text: String): VoiceIntent {
            val normalized = text.lowercase().trim()

            return when {
                // Navigation intents
                normalized.contains("next") ||
                        normalized.contains("continue") ||
                        normalized.contains("go on") ||
                        normalized.contains("proceed") -> NextStep

                normalized.contains("back") ||
                        normalized.contains("previous") ||
                        normalized.contains("go back") -> PreviousStep

                normalized.contains("repeat") ||
                        normalized.contains("again") ||
                        normalized.contains("say again") -> RepeatStep

                // Metronome control
                normalized.contains("start metronome") ||
                        normalized.contains("begin metronome") -> StartMetronome

                normalized.contains("stop metronome") ||
                        normalized.contains("end metronome") -> StopMetronome

                // Help and emergency
                normalized.contains("help") ||
                        normalized.contains("what") -> Help

                normalized.contains("call 911") ||
                        normalized.contains("call nine one one") ||
                        normalized.contains("emergency") -> Call911

                // Step numbers
                normalized.matches(Regex(".*step\\s+(\\d+).*")) -> {
                    val stepNum = Regex("step\\s+(\\d+)")
                        .find(normalized)
                        ?.groupValues
                        ?.get(1)
                        ?.toIntOrNull()

                    if (stepNum != null) GoToStep(stepNum) else Unknown(text)
                }

                normalized.matches(Regex(".*go to\\s+(\\d+).*")) -> {
                    val stepNum = Regex("go to\\s+(\\d+)")
                        .find(normalized)
                        ?.groupValues
                        ?.get(1)
                        ?.toIntOrNull()

                    if (stepNum != null) GoToStep(stepNum) else Unknown(text)
                }

                else -> Unknown(text)
            }
        }
    }
}