package com.voxaid.feature.instruction.emergency

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voxaid.core.content.model.EmergencyStep
import com.voxaid.core.design.components.Call911Dialog
import com.voxaid.core.design.components.GifImage
import com.voxaid.core.design.components.VoxAidTopBar
import com.voxaid.core.design.util.AnimationConfig
import com.voxaid.feature.instruction.components.MetronomeWithTone
import com.voxaid.feature.instruction.emergency.components.*
import timber.log.Timber

/**
 * Emergency Mode instruction screen with button/voice-only navigation.
 *
 * NO SWIPE NAVIGATION - Emergency protocols require:
 * - Voice confirmation at decision points
 * - Conditional branching based on patient status
 * - Dialog-driven decisions affecting protocol flow
 * - Coordinated TTS output for each step
 *
 * Navigation methods:
 * - Voice commands (primary for hands-free operation)
 * - Manual buttons (backup/accessibility)
 * - Programmatic (timers, auto-advance)
 */
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
    val isMetronomeActive by viewModel.isMetronomeActive.collectAsStateWithLifecycle()
    val show911Dialog by viewModel.show911Dialog.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val voiceHint by viewModel.showVoiceHint.collectAsStateWithLifecycle()
    val ttsEnabled by viewModel.ttsEnabled.collectAsStateWithLifecycle()
    val showPathSelection by viewModel.showPathSelection.collectAsStateWithLifecycle()
    val heimlichPath by viewModel.heimlichPath.collectAsStateWithLifecycle()
    val showLoopDialog by viewModel.showLoopDialog.collectAsStateWithLifecycle()
    val showSuccessDialog by viewModel.showSuccessDialog.collectAsStateWithLifecycle()

    // CPR Timer states
    val compressionCycleTime by viewModel.compressionCycleTime.collectAsStateWithLifecycle()
    val showSwitchWarning by viewModel.showSwitchWarning.collectAsStateWithLifecycle()
    val showRescuerDialog by viewModel.showRescuerDialog.collectAsStateWithLifecycle()
    val showContinueDialog by viewModel.showContinueDialog.collectAsStateWithLifecycle()
    val showSwitchOverlay by viewModel.showSwitchOverlay.collectAsStateWithLifecycle()
    val showExhaustionMessage by viewModel.showExhaustionMessage.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LaunchedEffect(audioState.asrReady) {
        if (audioState.asrReady) {
            Timber.d("🎤 ASR ready, starting listening")
            viewModel.startListening()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopListening()
        }
    }

    // Dialog handlers
    if (showPathSelection) {
        HeimlichPathSelectionDialog(
            onSelfSelected = { viewModel.selectHeimlichPath("self") },
            onHelpingSelected = { viewModel.selectHeimlichPath("helping") }
        )
        return
    }

    if (showLoopDialog) {
        HeimlichStillChokingDialog(
            isSelfPath = heimlichPath == "self",
            onStillChoking = { viewModel.onStillChoking() },
            onClear = { viewModel.onChokingCleared() }
        )
    }

    if (showSuccessDialog) {
        HeimlichSuccessDialog(
            isSelfPath = heimlichPath == "self",
            onComplete = {
                viewModel.completeHeimlichSession()
                onBackClick()
            }
        )
    }

    if (show911Dialog) {
        Call911Dialog(
            onConfirm = {
                viewModel.dismiss911Dialog()
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = "tel:911".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                runCatching { context.startActivity(intent) }
            },
            onDismiss = { viewModel.dismiss911Dialog() }
        )
    }

    showPopup?.let { popup ->
        EmergencyPopupDialog(
            title = popup.title,
            message = popup.message,
            yesLabel = popup.yesLabel,
            noLabel = popup.noLabel,
            onYes = { viewModel.handlePopupYes() },
            onNo = { viewModel.handlePopupNo() },
            onDismiss = { viewModel.dismissPopup() }
        )
    }

    if (showRescuerDialog) {
        RescuerSwitchDialog(
            onAlone = { viewModel.onAlone() },
            onWithHelp = { viewModel.onWithHelp() },
            onDismiss = { /* Cannot dismiss during emergency */ }
        )
    }

    if (showContinueDialog) {
        ContinueDialog(
            onContinue = { viewModel.onContinueCompressions() },
            onStop = { viewModel.onStopExhausted() },
            onDismiss = { /* Cannot dismiss during emergency */ }
        )
    }

    if (showExhaustionMessage) {
        ExhaustionMessage(
            onEnd = {
                viewModel.onEndSession()
                onBackClick()
            }
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
                        isMicActive = audioState.isListening && audioState.micPermissionGranted,
                        show911Button = true,
                        on911Click = { viewModel.show911Dialog() },
                        showTtsToggle = true,
                        ttsEnabled = ttsEnabled,
                        onTtsToggle = { viewModel.toggleTts() }
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
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is EmergencyUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                is EmergencyUiState.Success -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Main content - single step display
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                currentStep?.let { step ->
                                    EmergencyStepContent(
                                        step = step,
                                        elapsedTime = elapsedTime,
                                        beatCount = beatCount,
                                        voiceHint = voiceHint,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            // Metronome
                            if (isMetronomeActive) {
                                MetronomeWithTone(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    bpm = state.protocol.metronomeBpm ?: 110,
                                    isPlaying = true,
                                    onBeat = { viewModel.onMetronomeBeat() }
                                )
                            }

                            // Controls
                            EmergencyControls(
                                onBack = { viewModel.previousStep() },
                                onRepeat = { viewModel.repeatStep() },
                                onNext = { viewModel.nextStep() },
                                voiceHint = voiceHint,
                                isListening = audioState.isListening,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // CPR Timer Display (top-right corner)
                        if (compressionCycleTime > 0) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TimerDisplay(
                                    elapsedSeconds = compressionCycleTime,
                                    showWarning = showSwitchWarning
                                )

                                SwitchWarningBanner(
                                    visible = showSwitchWarning
                                )
                            }
                        }

                        // Switching Rescuer Overlay
                        if (showSwitchOverlay) {
                            SwitchingRescuerOverlay(
                                onComplete = { /* Handled by timer */ }
                            )
                        }
                    }
                }

                is EmergencyUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Unable to Load Protocol",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = state.message)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyStepContent(
    step: EmergencyStep,
    elapsedTime: Int,
    beatCount: Int,
    voiceHint: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = step.title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        step.animationResource?.let { animRes ->
            val resourceId = AnimationConfig.getAnimationResource(animRes)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (resourceId == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GifBox,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        GifImage(
                            resourceId = resourceId,
                            contentDescription = step.title,
                            modifier = Modifier.fillMaxSize(),
                            showPlaceholder = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        when (step) {
            is EmergencyStep.Instruction -> step.criticalWarning
            else -> null
        }?.let { warning ->
            Spacer(modifier = Modifier.height(20.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = warning,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        VoicePromptCard(
            step = step,
            voiceHint = voiceHint
        )
    }
}

@Composable
private fun VoicePromptCard(
    step: EmergencyStep,
    voiceHint: String?
) {
    val isPrimaryStep = step is EmergencyStep.VoiceTrigger || step is EmergencyStep.Popup

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimaryStep)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPrimaryStep) 4.dp else 1.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = if (isPrimaryStep)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(22.dp)
                        .padding(end = 6.dp)
                )
                Text(
                    text = when (step) {
                        is EmergencyStep.VoiceTrigger -> "Say one of these:"
                        is EmergencyStep.Popup -> "Say YES or NO:"
                        else -> "Voice commands active"
                    },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            when (step) {
                is EmergencyStep.VoiceTrigger -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        step.expectedKeywords.forEach { keyword ->
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = keyword.uppercase(),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                is EmergencyStep.Popup -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    val pulse = remember { Animatable(1f) }

                    LaunchedEffect(Unit) {
                        while (true) {
                            pulse.animateTo(1.1f, tween(600))
                            pulse.animateTo(1f, tween(600))
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.scale(pulse.value)
                    ) {
                        Text(
                            text = "YES",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = "NO",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                else -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Listening for your voice...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            voiceHint?.let {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 0.8.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = "💡 $it",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "👆 Tap buttons • 🎤 Use voice",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}