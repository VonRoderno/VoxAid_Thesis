package com.voxaid.feature.instruction.emergency

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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
import com.voxaid.feature.instruction.emergency.components.*
import timber.log.Timber
import androidx.core.net.toUri
import kotlinx.coroutines.delay

/**
 * Emergency Mode instruction screen with progress tracking and completion feedback.
 *
 * New Features (Phase 2):
 * - Horizontal step progress indicator
 * - Completion tracking with visual feedback
 * - Enhanced accessibility (TalkBack announcements)
 * - Smooth step transition animations
 * - Quick reference command hints
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
    val showTimerPopup by viewModel.showTimerPopup.collectAsStateWithLifecycle()
    val isMetronomeActive by viewModel.isMetronomeActive.collectAsStateWithLifecycle()
    val show911Dialog by viewModel.show911Dialog.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val voiceHint by viewModel.showVoiceHint.collectAsStateWithLifecycle()


    // Track completed steps for progress indicator
    val completedSteps = remember { mutableStateSetOf<Int>() }
    var previousStepId by remember { mutableStateOf<String?>(null) }
    var showCompletionOverlay by remember { mutableStateOf(false) }
    var completedStepTitle by remember { mutableStateOf("") }

    // Track step completion
    LaunchedEffect(currentStep?.stepId) {
        currentStep?.let { step ->
            if (previousStepId != null && previousStepId != step.stepId) {
                // Mark previous step as completed
                previousStepId?.let {
                    // Show completion feedback for non-terminal steps
                    if (step !is EmergencyStep.Terminal) {
                        completedStepTitle = previousStepId ?: ""
                        showCompletionOverlay = true
                    }
                }
            }
            previousStepId = step.stepId
        }
    }

    LaunchedEffect(audioState.asrReady) {
        if (audioState.asrReady) {
            Timber.d("🎤 ASR ready, starting listening")
            viewModel.startListening()
        } else {
            Timber.d("⏳ Waiting for ASR to become ready...")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopListening()
        }
    }

    val context = LocalContext.current

    // Handle 911 dialog
    if (show911Dialog) {
        Call911Dialog(
            onConfirm = {
                viewModel.dismiss911Dialog()
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = "tel:911".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to launch dialer")
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

    // Step completion overlay
    if (showCompletionOverlay) {
        StepCompletionOverlay(
            stepTitle = completedStepTitle,
            onDismiss = { showCompletionOverlay = false }
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
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f)
                        )
                    )
                )
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

                        // Protocol header
//                        ProtocolHeaderCard(
//                            protocolName = state.protocol.name,
//                            warning = state.protocol.warning
//                        )

                        // Progress indicator
                        EmergencyProgressIndicator(
                            currentStepIndex = 0, // TODO: Track actual step index
                            totalSteps = 10, // TODO: Get actual total from protocol
                            completedSteps = completedSteps
                        )

                        // Current step content with animated transitions
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures { change, dragAmount ->
                                        change.consume()
                                        if (dragAmount < -50) viewModel.nextStep()
                                        else if (dragAmount > 50) viewModel.previousStep()
                                    }
                                }
                                .semantics {
                                    liveRegion = LiveRegionMode.Assertive
                                }
                        ) {
                            AnimatedContent(
                                targetState = currentStep,
                                transitionSpec = {
                                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                                            slideOutHorizontally { width -> -width } + fadeOut()
                                },
                                label = "step_transition"
                            ) { step ->
                                step?.let {
                                    EmergencyStepContent(
                                        step = it,
                                        elapsedTime = elapsedTime,
                                        beatCount = beatCount,
                                        voiceHint = voiceHint,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                        // Metronome (for CPR)
                        if (isMetronomeActive) {
                            Metronome(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                bpm = state.protocol.metronomeBpm ?: 110,
                                isPlaying = true,
                                onBeat = { viewModel.onMetronomeBeat() }
                            )
                        }



                        // Control buttons
                        val showNextButton = when (currentStep) {
                            is EmergencyStep.Instruction, is EmergencyStep.Timed, is EmergencyStep.Loop -> true
                            else -> false
                        }
                        val showBackButton = currentStep !is EmergencyStep.Terminal

                        EmergencyControls(
                            onBack = { viewModel.previousStep() },
                            onRepeat = { viewModel.repeatStep() },
                            onNext = { viewModel.nextStep() },
                            showNext = showNextButton,
                            showBack = showBackButton,
                            voiceHint = voiceHint,
                            isListening = audioState.isListening,
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

// ProtocolHeaderCard and EmergencyStepContent remain the same as Phase 1
// (Included in previous phase - no changes needed)

@Composable
private fun ProtocolHeaderCard(
    protocolName: String,
    warning: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocalHospital,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = protocolName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            warning?.let {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(
                    Modifier,
                    DividerDefaults.Thickness,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
                                text = "GIF Placeholder",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = animRes,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(8.dp),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "📁 Replace in:\napp/src/main/res/raw/",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "600px • 1:1 ratio • 12-18fps • <1.2MB",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Text(
                text = step.description,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.headlineSmall.lineHeight.times(1.4f),
                modifier = Modifier.padding(20.dp)
            )
        }

        if (step is EmergencyStep.Timed) {
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (step.countBeats && step.targetBeats != null) {
                        Text(
                            text = "$beatCount",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "of ${step.targetBeats} compressions",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
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
                        Text(
                            text = "elapsed",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (step) {
                is EmergencyStep.VoiceTrigger, is EmergencyStep.Popup ->
                    MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = when (step) {
                        is EmergencyStep.VoiceTrigger, is EmergencyStep.Popup ->
                            MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (step) {
                        is EmergencyStep.VoiceTrigger -> "Say one of these:"
                        is EmergencyStep.Popup -> "Say YES or NO:"
                        else -> "Voice commands active"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            when (step) {
                is EmergencyStep.VoiceTrigger -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = step.expectedKeywords.joinToString("  •  ").uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
                is EmergencyStep.Popup -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "YES",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "NO",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {}
            }

            voiceHint?.let {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )
                Text(
                    text = "💡 $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "👆 Swipe or use buttons if voice isn't working",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}