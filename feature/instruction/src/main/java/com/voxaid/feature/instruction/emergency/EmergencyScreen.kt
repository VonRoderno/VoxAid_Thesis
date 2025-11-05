package com.voxaid.feature.instruction.emergency

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voxaid.core.content.model.EmergencyStep
import com.voxaid.core.design.components.Call911Dialog
import com.voxaid.core.design.components.GifImage
import com.voxaid.core.design.components.VoxAidTopBar
import com.voxaid.core.design.theme.VoxAidTheme
import com.voxaid.core.design.util.AnimationConfig
import com.voxaid.feature.instruction.components.Metronome
import com.voxaid.feature.instruction.emergency.components.EmergencyControls
import com.voxaid.feature.instruction.emergency.components.EmergencyPopupDialog
import timber.log.Timber

/**
 * Emergency Mode instruction screen.
 *
 * Updated: Added swipe gesture and manual navigation controls
 * Features:
 * - Horizontal swipe to navigate
 * - Manual Next/Back/Repeat buttons
 * - Voice commands still work
 * - Fallback when voice recognition fails
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmergencyScreen(
    onBackClick: () -> Unit,
    viewModel: EmergencyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    val elapsedTime by viewModel.elapsedTime.collectAsStateWithLifecycle()
    val beatCount by viewModel.beatCount.collectAsStateWithLifecycle()
    val showPopup by viewModel.showPopup.collectAsStateWithLifecycle()
    val showTimerPopup by viewModel.showTimerPopup.collectAsStateWithLifecycle()
    val isMetronomeActive by viewModel.isMetronomeActive.collectAsStateWithLifecycle()
    val show911Dialog by viewModel.show911Dialog.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val voiceHint by viewModel.showVoiceHint.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Start listening when screen appears
    LaunchedEffect(Unit) {
        viewModel.startListening()
    }

    // Stop listening when screen disappears
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopListening()
        }
    }

    // Handle 911 dialog
    if (show911Dialog) {
        Call911Dialog(
            onConfirm = {
                viewModel.dismiss911Dialog()
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:911")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    timber.log.Timber.e(e, "Failed to launch dialer")
                }
            },
            onDismiss = { viewModel.dismiss911Dialog() }
        )
    }

    // Handle popup dialogs
    showPopup?.let { popup ->
        EmergencyPopupDialog(
            title = popup.title,
            message = popup.message,
            yesLabel = popup.yesLabel,
            noLabel = popup.noLabel,
            onYes = { viewModel.selectPopupYesManually() },
            onNo = { viewModel.selectPopupNoManually() },
            onDismiss = { viewModel.dismissPopup() }
        )
    }

    // Handle timer popups
    showTimerPopup?.let { timerPopup ->
        EmergencyPopupDialog(
            title = timerPopup.title,
            message = timerPopup.message,
            yesLabel = timerPopup.options.getOrNull(0)?.label ?: "Yes",
            noLabel = timerPopup.options.getOrNull(1)?.label ?: "No",
            onYes = {
                viewModel.handleTimerPopupResponse(
                    timerPopup.options.getOrNull(0)?.response ?: TimerPopupResponse.Continue
                )
            },
            onNo = {
                viewModel.handleTimerPopupResponse(
                    timerPopup.options.getOrNull(1)?.response ?: TimerPopupResponse.Continue
                )
            },
            onDismiss = { /* Timer popups cannot be dismissed */ }
        )
    }

    Scaffold(
        topBar = {
            when (val state = uiState) {
                is EmergencyUiState.Success -> {
                    VoxAidTopBar(
                        title = "🚨 ${state.protocol.name}",
                        onBackClick = {
                            viewModel.stopTts()
                            onBackClick()
                        },
                        showMicIndicator = true,
                        isMicActive = audioState.isListening,
                        show911Button = true,
                        on911Click = { viewModel.show911Dialog() }
                    )
                }
                else -> {
                    VoxAidTopBar(
                        title = "🚨 Emergency Mode",
                        onBackClick = onBackClick,
                        show911Button = true,
                        on911Click = { viewModel.show911Dialog() }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
    ) { paddingValues ->
        // Emergency red border
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .border(
                    width = 4.dp,
                    color = MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(0.dp)
                )
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.05f))
        ) {
            when (val state = uiState) {
                is EmergencyUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                is EmergencyUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // Warning banner
                        state.protocol.warning?.let { warning ->
                            EmergencyWarningBanner(warning)
                        }

                        // Metronome (for CPR)
                        if (isMetronomeActive) {
                            Metronome(
                                bpm = state.protocol.metronomeBpm ?: 110,
                                isPlaying = true,
                                onBeat = { viewModel.onMetronomeBeat() },
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        // Current step content with swipe support
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures { change, dragAmount ->
                                        change.consume()

                                        // Swipe left = next, swipe right = back
                                        if (dragAmount < -50) {
                                            viewModel.nextStep()
                                        } else if (dragAmount > 50) {
                                            viewModel.previousStep()
                                        }
                                    }
                                }
                        ) {
                            currentStep?.let { step ->
                                EmergencyStepContent(
                                    step = step,
                                    elapsedTime = elapsedTime,
                                    beatCount = beatCount,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Control buttons - FIXED: Properly determine when to show NEXT button
                        val currentStepType = currentStep?.javaClass?.simpleName
                        val isVoiceTriggerStep = currentStep is EmergencyStep.VoiceTrigger
                        val isPopupStep = currentStep is EmergencyStep.Popup
                        val isTerminalStep = currentStep is EmergencyStep.Terminal

                        // Show NEXT button for instruction and timed steps
                        // Hide for voice triggers, popups, and terminal steps
                        val showNextButton = when (currentStep) {
                            is EmergencyStep.Instruction -> true
                            is EmergencyStep.Timed -> true
                            is EmergencyStep.Loop -> true
                            is EmergencyStep.VoiceTrigger -> false // Wait for voice
                            is EmergencyStep.Popup -> false // Wait for popup selection
                            is EmergencyStep.Terminal -> false // End of protocol
                            null -> false
                        }

                        val showBackButton = when (currentStep) {
                            is EmergencyStep.Terminal -> false // Can't go back from end
                            else -> true // Allow back on all other steps
                        }

                        Timber.d("Button visibility - Type: $currentStepType, Next: $showNextButton, Back: $showBackButton")

                        EmergencyControls(
                            onBack = { viewModel.previousStep() },
                            onRepeat = { viewModel.repeatStep() },
                            onNext = { viewModel.nextStep() },
                            showNext = showNextButton,
                            showBack = showBackButton,
                            voiceHint = voiceHint,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                is EmergencyUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⚠️ Error",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyWarningBanner(warning: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.error
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Text(
            text = warning,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onError,
            modifier = Modifier.padding(20.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmergencyStepContent(
    step: EmergencyStep,
    elapsedTime: Int,
    beatCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step title - LARGE for emergency
        Text(
            text = step.title,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // GIF animation
        step.animationResource?.let { animRes ->
            val resourceId = AnimationConfig.getAnimationResource(animRes)
            GifImage(
                resourceId = resourceId,
                contentDescription = step.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                showPlaceholder = resourceId == null
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Step description - LARGE text
        Text(
            text = step.description,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            lineHeight = MaterialTheme.typography.headlineMedium.lineHeight.times(1.4f)
        )

        // Timer/beat display for timed steps
        if (step is EmergencyStep.Timed) {
            Spacer(modifier = Modifier.height(24.dp))

            if (step.countBeats && step.targetBeats != null) {
                Text(
                    text = "$beatCount / ${step.targetBeats}",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "compressions",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val minutes = elapsedTime / 60
                val seconds = elapsedTime % 60
                Text(
                    text = String.format("%d:%02d", minutes, seconds),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Critical warning
        when (step) {
            is EmergencyStep.Instruction -> step.criticalWarning
            else -> null
        }?.let { warning ->
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚠️ $warning",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        // Voice prompt hint - show expected keywords
        Spacer(modifier = Modifier.height(24.dp))

        when (step) {
            is EmergencyStep.VoiceTrigger -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎤 SAY ONE OF THESE:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = step.expectedKeywords.joinToString("  •  ").uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Or use the NEXT button below if voice isn't working",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            is EmergencyStep.Popup -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎤 SAY YOUR ANSWER:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "YES  •  NO",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Or tap YES/NO in the popup dialog",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            else -> {
                Text(
                    text = "🎤 Voice commands active  •  👆 Swipe or use buttons to navigate",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmergencyScreenPreview() {
    VoxAidTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))
                .border(4.dp, MaterialTheme.colorScheme.error)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Survey the Scene",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Look around. Is it safe to approach?",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎤 SAY ONE OF THESE:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "SAFE  •  CLEAR  •  OKAY",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
