package com.voxaid.feature.instruction

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voxaid.core.design.components.Call911Dialog
import com.voxaid.core.design.components.VoxAidTopBar
import com.voxaid.core.design.theme.VoxAidTheme
import com.voxaid.feature.instruction.components.*
import timber.log.Timber

/**
 * Instruction screen for displaying protocol steps.
 * Handles both instructional and emergency modes.
 * Tracks completion and unlocks emergency mode variants.
 */
@Composable
fun InstructionScreen(
    onBackClick: () -> Unit,
    viewModel: InstructionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentStepIndex by viewModel.currentStepIndex.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val isMetronomeActive by viewModel.isMetronomeActive.collectAsStateWithLifecycle()
    val show911Dialog by viewModel.show911Dialog.collectAsStateWithLifecycle()
    val isEmergencyMode = viewModel.isEmergencyMode

    val context = LocalContext.current

    // Start listening when screen appears
    LaunchedEffect(audioState.asrReady) {
        if (audioState.asrReady) {
            Timber.d("🎤 ASR ready, starting listening")
            viewModel.startListening()
        } else {
            Timber.d("⏳ Waiting for ASR to become ready...")
        }
    }

    // Stop listening when screen disappears
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopListening()
        }
    }

    // Handle 911 dialog - can be triggered by voice command or button tap
    if (show911Dialog) {
        Call911Dialog(
            onConfirm = {
                viewModel.dismiss911Dialog()
                // Launch dialer with 911
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:911")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Handle case where dialer is not available
                    Timber.e(e, "Failed to launch dialer")
                }
            },
            onDismiss = { viewModel.dismiss911Dialog() }
        )
    }

    Scaffold(
        topBar = {
            when (val state = uiState) {
                is InstructionUiState.Success -> {
                    VoxAidTopBar(
                        title = state.protocol.name,
                        onBackClick = {
                            viewModel.stopTts()
                            onBackClick()
                        },
                        showMicIndicator = true,
                        isMicActive = audioState.isListening && audioState.micPermissionGranted,
                        show911Button = true,
                        on911Click = { viewModel.show911Dialog() }
                    )
                }

                else -> {
                    VoxAidTopBar(
                        title = "Loading...",
                        onBackClick = onBackClick,
                        show911Button = true,
                        on911Click = { viewModel.show911Dialog() }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        val emergencyBorder = if (isEmergencyMode)
            Modifier
                .padding(paddingValues)
                .border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(0.dp)
                )
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.05f))
        else
            Modifier.padding(paddingValues)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(emergencyBorder)
        ){
            when (val state = uiState) {
                is InstructionUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is InstructionUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // Protocol-level warning banner
//                        state.protocol.warning?.let { warning ->
//                            BorderedInfoBanner(
//                                text = warning,
//                                type = if (isEmergencyMode) BannerType.Warning else BannerType.Info,
//                                modifier = Modifier.padding(16.dp)
//                            )
//                        }
//
//// Step-specific emergency warning
//                        if (isEmergencyMode && state.currentStep.criticalWarning != null) {
//                            BorderedInfoBanner(
//                                text = state.currentStep.criticalWarning!!,
//                                type = BannerType.Critical,
//                                modifier = Modifier.padding(horizontal = 16.dp)
//                            )
//                        }

                        // Metronome for CPR (when applicable)
//                        if (state.protocol.id == "cpr_learning" && state.protocol.metronomeBpm != null) {
//                            Metronome(
//                                bpm = state.protocol.metronomeBpm!!,
//                                isPlaying = isMetronomeActive,
//                                onBeat = {},
//                                modifier = Modifier.padding(16.dp)
//                            )
//                        }

                        // Auto-advance timer for emergency mode
                        if (isEmergencyMode && state.currentStep.durationSeconds != null) {
                            AutoAdvanceTimer(
                                durationSeconds = state.currentStep.durationSeconds!!,
                                onComplete = { viewModel.nextStep() },
                                onCancel = {},
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Step content
                        InstructionStepPager(
                            protocol = state.protocol,
                            currentStepIndex = currentStepIndex,
                            isEmergencyMode = isEmergencyMode,
                            onPageChanged = { newIndex ->
                                viewModel.goToStep(newIndex)
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Step controls
                        StepControls(
                            currentStepIndex = currentStepIndex,
                            totalSteps = state.protocol.steps.size,
                            isEmergencyMode = isEmergencyMode,
                            onPreviousClick = { viewModel.previousStep() },
                            onNextClick = { viewModel.nextStep() },
                            onRepeatClick = { viewModel.repeatStep() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                is InstructionUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error Loading Protocol",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BorderedInfoBanner(
    text: String,
    type: BannerType,
    modifier: Modifier = Modifier
) {
    val color = when (type) {
        BannerType.Info -> MaterialTheme.colorScheme.primary
        BannerType.Warning -> MaterialTheme.colorScheme.error
        BannerType.Critical -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(2.dp, color),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (type) {
                    BannerType.Info -> "💡"
                    BannerType.Warning -> "⚠️"
                    BannerType.Critical -> "🚨"
                },
                color = color,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

enum class BannerType { Info, Warning, Critical }
@Composable
private fun WarningBanner(warning: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = "⚠️ $warning",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WarningBannerPreview() {
    VoxAidTheme {
        WarningBanner("Only perform CPR if the person is unresponsive and not breathing. Call 911 immediately.")
    }
}