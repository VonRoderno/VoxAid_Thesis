package com.voxaid.feature.instruction

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
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
    LaunchedEffect(Unit) {
        viewModel.startListening()
    }

    // Stop listening when screen disappears
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopListening()
        }
    }

    // Handle 911 dialog triggered by voice command
    if (show911Dialog) {
        Call911Dialog(
            onConfirm = {
                viewModel.dismiss911Dialog()
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:911")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
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
                        isMicActive = audioState.isListening
                    )
                }

                else -> {
                    VoxAidTopBar(
                        title = "Loading...",
                        onBackClick = onBackClick
                    )
                }
            }
        },
        containerColor = if (isEmergencyMode) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.background
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is InstructionUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is InstructionUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // Warning banner if present
                        state.protocol.warning?.let { warning ->
                            WarningBanner(warning)
                        }

                        // Emergency-specific critical warning
                        if (isEmergencyMode && state.currentStep.criticalWarning != null) {
                            EmergencyBanner(
                                message = state.currentStep.criticalWarning!!,
                                type = EmergencyBannerType.Critical,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        // Metronome for CPR (when applicable)
                        if (state.protocol.id == "cpr" && state.protocol.metronomeBpm != null) {
                            Metronome(
                                bpm = state.protocol.metronomeBpm!!,
                                isPlaying = isMetronomeActive,
                                onBeat = {},
                                modifier = Modifier.padding(16.dp)
                            )
                        }

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
