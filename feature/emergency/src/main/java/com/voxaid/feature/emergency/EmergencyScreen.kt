//package com.voxaid.feature.emergency
//
//import android.content.Intent
//import android.net.Uri
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import com.voxaid.core.content.model.EmergencyStep
//import com.voxaid.core.design.components.Call911Dialog
//import com.voxaid.core.design.components.VoxAidTopBar
//import com.voxaid.core.design.theme.VoxAidTheme
//import com.voxaid.feature.emergency.components.*
//import timber.log.Timber
//
///**
// * Emergency Mode screen.
// * Renders different UI components based on current step type.
// *
// * State Machine Rendering:
// * - InstructionStep → InstructionStepCard
// * - VoicePromptStep → VoicePromptCard
// * - PopupDecisionStep → PopupDecisionDialog
// * - TimedActionStep → TimedCompressionCard
// * - CompletionStep → CompletionCard
// */
//@Composable
//fun EmergencyScreen(
//    protocolId: String,
//    onBackClick: () -> Unit,
//    onComplete: () -> Unit,
//    viewModel: EmergencyViewModel = hiltViewModel()
//) {
//    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
//    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
//
//    val context = LocalContext.current
//    var show911Dialog by remember { mutableStateOf(false) }
//
//    // Start listening when screen appears
//    LaunchedEffect(Unit) {
//        viewModel.startListening()
//    }
//
//    // Stop listening when screen disappears
//    DisposableEffect(Unit) {
//        onDispose {
//            viewModel.stopListening()
//        }
//    }
//
//    // Handle 911 dialog
//    if (show911Dialog) {
//        Call911Dialog(
//            onConfirm = {
//                show911Dialog = false
//                val intent = Intent(Intent.ACTION_DIAL).apply {
//                    data = Uri.parse("tel:911")
//                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                }
//                try {
//                    context.startActivity(intent)
//                } catch (e: Exception) {
//                    Timber.e(e, "Failed to launch dialer")
//                }
//            },
//            onDismiss = { show911Dialog = false }
//        )
//    }
//
//    Scaffold(
//        topBar = {
//            when (val state = uiState) {
//                is EmergencyUiState.Active -> {
//                    VoxAidTopBar(
//                        title = "🚨 ${state.protocol.name}",
//                        onBackClick = {
//                            viewModel.stopListening()
//                            onBackClick()
//                        },
//                        showMicIndicator = true,
//                        isMicActive = audioState.isListening,
//                        show911Button = true,
//                        on911Click = { show911Dialog = true }
//                    )
//                }
//                else -> {
//                    VoxAidTopBar(
//                        title = "Emergency Mode",
//                        onBackClick = onBackClick,
//                        show911Button = true,
//                        on911Click = { show911Dialog = true }
//                    )
//                }
//            }
//        },
//        containerColor = MaterialTheme.colorScheme.errorContainer
//    ) { paddingValues ->
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//        ) {
//            when (val state = uiState) {
//                is EmergencyUiState.Loading -> {
//                    CircularProgressIndicator(
//                        modifier = Modifier.align(Alignment.Center),
//                        color = MaterialTheme.colorScheme.error
//                    )
//                }
//
//                is EmergencyUiState.Active -> {
//                    // Render step based on type
//                    RenderCurrentStep(
//                        state = state,
//                        viewModel = viewModel,
//                        onComplete = onComplete
//                    )
//
//                    // Show popup if active
//                    state.activePopup?.let { popup ->
//                        PopupDecisionDialog(
//                            popup = popup,
//                            onOptionSelected = { option ->
//                                viewModel.onPopupOptionSelected(option)
//                            },
//                            onDismiss = { /* Popups are not dismissible */ }
//                        )
//                    }
//                }
//
//                is EmergencyUiState.Error -> {
//                    Column(
//                        modifier = Modifier
//                            .align(Alignment.Center)
//                            .padding(32.dp),
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        Text(
//                            text = "Error",
//                            style = MaterialTheme.typography.headlineMedium,
//                            color = MaterialTheme.colorScheme.error
//                        )
//                        Spacer(modifier = Modifier.height(16.dp))
//                        Text(
//                            text = state.message,
//                            style = MaterialTheme.typography.bodyLarge,
//                            color = MaterialTheme.colorScheme.onErrorContainer
//                        )
//                        Spacer(modifier = Modifier.height(24.dp))
//                        Button(onClick = onBackClick) {
//                            Text("Go Back")
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
///**
// * Renders the appropriate UI component for the current step type.
// */
//@Composable
//private fun RenderCurrentStep(
//    state: EmergencyUiState.Active,
//    viewModel: EmergencyViewModel,
//    onComplete: () -> Unit
//) {
//    when (val step = state.currentStep) {
//        is EmergencyStep.InstructionStep -> {
//            InstructionStepCard(
//                step = step,
//                elapsedSeconds = state.elapsedSeconds,
//                onContinue = { viewModel.advanceStep() },
//                onRepeat = { viewModel.repeatCurrentStep() }
//            )
//        }
//
//        is EmergencyStep.VoicePromptStep -> {
//            VoicePromptCard(
//                step = step,
//                isListening = state.voiceListening,
//                onManualContinue = { viewModel.advanceStep() },
//                onRepeat = { viewModel.repeatCurrentStep() }
//            )
//        }
//
//        is EmergencyStep.TimedActionStep -> {
//            TimedCompressionCard(
//                step = step,
//                currentCount = state.compressionCount,
//                isMetronomeActive = state.isMetronomeActive,
//                metronomeBpm = state.protocol.metronomeBpm ?: 110,
//                onRepeat = { viewModel.repeatCurrentStep() }
//            )
//        }
//
//        is EmergencyStep.CompletionStep -> {
//            CompletionCard(
//                step = step,
//                totalElapsedSeconds = state.elapsedSeconds,
//                onFinish = onComplete
//            )
//        }
//
//        is EmergencyStep.PopupDecisionStep -> {
//            // Popup is rendered separately in parent
//            // Show loading indicator while popup is being shown
//            Box(
//                modifier = Modifier.fillMaxSize(),
//                contentAlignment = Alignment.Center
//            ) {
//                CircularProgressIndicator(
//                    color = MaterialTheme.colorScheme.error
//                )
//            }
//        }
//        is EmergencyStep.LoopControlStep -> {
//            // Loop control is handled automatically by ViewModel
//            // Show loading while processing
//            Box(
//                modifier = Modifier.fillMaxSize(),
//                contentAlignment = Alignment.Center
//            ) {
//                CircularProgressIndicator(
//                    color = MaterialTheme.colorScheme.error
//                )
//            }
//        }
//    }
//}