//package com.voxaid.feature.emergency
//
//import com.voxaid.core.content.model.EmergencyProtocol
//import com.voxaid.core.content.model.EmergencyStep
//import com.voxaid.core.content.model.TimerTrigger
//
///**
// * UI state for Emergency Mode screen.
// * Manages current step, timer state, and popup visibility.
// */
//sealed class EmergencyUiState {
//    data object Loading : EmergencyUiState()
//
//    data class Active(
//        val protocol: EmergencyProtocol,
//        val currentStep: EmergencyStep,
//        val stepHistory: List<String> = emptyList(), // For back navigation
//        val elapsedSeconds: Int = 0, // Global timer for the entire protocol
//        val compressionCount: Int = 0, // For timed compression steps
//        val isMetronomeActive: Boolean = false,
//        val activePopup: PopupState? = null,
//        val voiceListening: Boolean = false,
//        val loopIteration: Int = 0 // Track number of CPR cycles
//    ) : EmergencyUiState()
//
//    data class Error(val message: String) : EmergencyUiState()
//}
//
///**
// * Popup state for decision dialogs.
// */
//data class PopupState(
//    val title: String,
//    val message: String,
//    val options: List<String>,
//    val optionActions: Map<String, String>, // Option -> Next step ID
//    val voiceEnabled: Boolean = true,
//    val source: PopupSource
//)
//
///**
// * Source of popup (for handling differently).
// */
//sealed class PopupSource {
//    data object StepBased : PopupSource() // From PopupDecisionStep
//    data class TimerTriggered(val trigger: TimerTrigger.ShowPopup) : PopupSource() // From timer
//}
//
///**
// * Events that can occur during emergency protocol execution.
// */
//sealed class EmergencyEvent {
//    data class StepCompleted(val stepId: String) : EmergencyEvent()
//    data class VoiceCommandReceived(val command: String) : EmergencyEvent()
//    data class PopupOptionSelected(val option: String) : EmergencyEvent()
//    data class TimerTick(val elapsedSeconds: Int) : EmergencyEvent()
//    data class CompressionCountIncremented(val count: Int) : EmergencyEvent()
//    data object MetronomeStarted : EmergencyEvent()
//    data object MetronomeStopped : EmergencyEvent()
//    data object BackPressed : EmergencyEvent()
//}