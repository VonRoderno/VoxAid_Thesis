package com.voxaid.core.audio.model

/**
 * Recognized voice intents from ASR.
 * Maps recognized speech to actionable commands.
 *
 * Updated: Added emergency-specific keywords (safe, clear, yes, no, etc.)
 */
sealed class VoiceIntent {
    data object NextStep : VoiceIntent()
    data object PreviousStep : VoiceIntent()
    data object RepeatStep : VoiceIntent()
    data object StartMetronome : VoiceIntent()
    data object StopMetronome : VoiceIntent()
    data object Help : VoiceIntent()
    data object Call911 : VoiceIntent()

    // Emergency-specific intents
    data object SafeClear : VoiceIntent() // "safe", "clear", "okay" for scene survey
    data object Yes : VoiceIntent() // "yes", "yeah", "affirmative"
    data object No : VoiceIntent() // "no", "nope", "negative"
    data object Responsive : VoiceIntent() // "responsive", "conscious"
    data object Unresponsive : VoiceIntent() // "unresponsive", "unconscious"
    data object Alone : VoiceIntent() // "alone", "by myself"
    data object NotAlone : VoiceIntent() // "not alone", "someone here"
    data object Continue : VoiceIntent() // "continue", "keep going"
    data object Stop : VoiceIntent() // "stop", "exhausted"

    data class GoToStep(val stepNumber: Int) : VoiceIntent()
    data class Unknown(val rawText: String) : VoiceIntent()

    companion object {
        /**
         * Parses raw recognized text into a VoiceIntent.
         * Updated with emergency-specific keywords.
         */
        fun fromText(text: String): VoiceIntent {
            val normalized = text.lowercase().trim()

            return when {
                // Emergency scene survey
                normalized.contains("safe") ||
                        normalized.contains("clear") ||
                        normalized.contains("okay") ||
                        normalized.contains("ok") ||
                        normalized.contains("save") -> SafeClear


//                normalized in listOf("safe", "clear", "okay", "ok", "safe to approach", "save") -> SafeClear

                // Yes/No responses
                normalized == "yes" ||
                        normalized == "yeah" ||
                        normalized == "yep" ||
                        normalized == "yup" ||
                        normalized == "affirmative" ||
                        normalized == "correct" -> Yes
                normalized == "no" ||
                        normalized == "nope" ||
                        normalized == "nah" ||
                        normalized == "negative" ||
                        normalized == "incorrect" ||
                        normalized == "whoa" -> No
//                normalized in listOf("yes", "yeah", "yep", "yup", "affirmative", "correct") -> Yes
//                normalized in listOf("no", "nope", "nah", "negative", "incorrect") -> No

                // Responsiveness check
                normalized == "responsive" ||
                        normalized == "conscious" ||
                        normalized == "awake" ||
                        normalized == "responding" ||
                        normalized == "responsible" -> Responsive

                normalized == "unresponsive" ||
                        normalized == "unconscious" ||
                        normalized == "not awake" ||
                        normalized == "not responding" ||
                        normalized == "not responsible" ||
                        normalized == "no response" -> Unresponsive

//                normalized in listOf("responsive", "conscious", "awake", "responding") -> Responsive
//                normalized in listOf("unresponsive", "unconscious", "not responding", "no response") -> Unresponsive

                // Alone status
                normalized in listOf("alone", "by myself", "solo", "just me") -> Alone
                normalized in listOf("not alone", "someone here", "help available", "partner here") -> NotAlone

                // Continue/Stop
                normalized in listOf("continue", "keep going", "go on", "proceed") -> Continue
                normalized in listOf("stop", "exhausted", "tired", "can't continue", "rest") -> Stop

                // Navigation intents
                normalized.contains("next") ||
                        normalized.contains("continue") && !normalized.contains("can't") ||
                        normalized.contains("go on") ||
                        normalized.contains("proceed")||
                        normalized.contains("makes")||
                        normalized.contains("mix") ||
                        normalized.contains("myths")||
                        normalized.contains("max") -> NextStep

                normalized.contains("back") ||
                        normalized.contains("previous") ||
                        normalized.contains("go back") ||
                        normalized.contains("buck") ||
                        normalized.contains("but")||
                        normalized.contains("bob")||
                        normalized.contains("mark") -> PreviousStep

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