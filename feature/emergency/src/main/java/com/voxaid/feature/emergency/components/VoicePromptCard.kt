//package com.voxaid.feature.emergency.components
//
//import androidx.compose.animation.core.*
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Mic
//import androidx.compose.material.icons.filled.Refresh
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.scale
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import com.voxaid.core.content.model.EmergencyStep
//import com.voxaid.core.design.theme.VoxAidTheme
//
///**
// * Voice prompt card for steps requiring specific voice commands.
// * Shows large, animated microphone indicator and expected commands.
// *
// * Used for: Scene safety confirmation ("Safe"), decision gates
// */
//@Composable
//fun VoicePromptCard(
//    step: EmergencyStep.VoicePromptStep,
//    isListening: Boolean,
//    onManualContinue: () -> Unit,
//    onRepeat: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    // Pulsing animation for microphone
//    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
//    val scale by infiniteTransition.animateFloat(
//        initialValue = 1f,
//        targetValue = 1.15f,
//        animationSpec = infiniteRepeatable(
//            animation = tween(800, easing = FastOutSlowInEasing),
//            repeatMode = RepeatMode.Reverse
//        ),
//        label = "scale"
//    )
//
//    Column(
//        modifier = modifier
//            .fillMaxSize()
//            .padding(24.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        // Animated microphone icon
//        Surface(
//            modifier = Modifier
//                .size(120.dp)
//                .scale(if (isListening) scale else 1f),
//            shape = MaterialTheme.shapes.extraLarge,
//            color = if (isListening) {
//                MaterialTheme.colorScheme.error
//            } else {
//                MaterialTheme.colorScheme.surfaceVariant
//            },
//            shadowElevation = if (isListening) 8.dp else 0.dp
//        ) {
//            Box(
//                contentAlignment = Alignment.Center,
//                modifier = Modifier.fillMaxSize()
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Mic,
//                    contentDescription = null,
//                    modifier = Modifier.size(64.dp),
//                    tint = if (isListening) {
//                        MaterialTheme.colorScheme.onError
//                    } else {
//                        MaterialTheme.colorScheme.onSurfaceVariant
//                    }
//                )
//            }
//        }
//
//        Spacer(modifier = Modifier.height(32.dp))
//
//        // Prompt text
//        Text(
//            text = step.promptText,
//            style = MaterialTheme.typography.displaySmall,
//            fontWeight = FontWeight.Bold,
//            color = MaterialTheme.colorScheme.onErrorContainer,
//            textAlign = TextAlign.Center,
//            lineHeight = MaterialTheme.typography.displaySmall.lineHeight.times(1.2f)
//        )
//
//        Spacer(modifier = Modifier.height(24.dp))
//
//        // Expected commands
//        Card(
//            colors = CardDefaults.cardColors(
//                containerColor = MaterialTheme.colorScheme.primaryContainer
//            ),
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Column(
//                modifier = Modifier.padding(20.dp),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Text(
//                    text = "Say one of these:",
//                    style = MaterialTheme.typography.titleMedium,
//                    color = MaterialTheme.colorScheme.onPrimaryContainer,
//                    fontWeight = FontWeight.SemiBold
//                )
//
//                Spacer(modifier = Modifier.height(12.dp))
//
//                step.expectedCommands.forEach { command ->
//                    Text(
//                        text = "\"${command.uppercase()}\"",
//                        style = MaterialTheme.typography.headlineSmall,
//                        color = MaterialTheme.colorScheme.primary,
//                        fontWeight = FontWeight.Bold
//                    )
//                    Spacer(modifier = Modifier.height(8.dp))
//                }
//            }
//        }
//
//        Spacer(modifier = Modifier.height(32.dp))
//
//        // Manual controls
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            OutlinedButton(
//                onClick = onRepeat,
//                modifier = Modifier.weight(1f)
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Refresh,
//                    contentDescription = null,
//                    modifier = Modifier.size(20.dp)
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                Text("Repeat")
//            }
//
//            Button(
//                onClick = onManualContinue,
//                modifier = Modifier.weight(1f),
//                colors = ButtonDefaults.buttonColors(
//                    containerColor = MaterialTheme.colorScheme.error
//                )
//            ) {
//                Text("Continue Anyway")
//            }
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Status text
//        Text(
//            text = if (isListening) {
//                "🎤 Listening for your voice..."
//            } else {
//                "Microphone ready"
//            },
//            style = MaterialTheme.typography.bodyMedium,
//            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
//            textAlign = TextAlign.Center
//        )
//
//        // Timeout info
//        step.timeoutSeconds?.let { timeout ->
//            Spacer(modifier = Modifier.height(8.dp))
//            Text(
//                text = "⏱️ Auto-continues in $timeout seconds",
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f),
//                textAlign = TextAlign.Center
//            )
//        }
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//private fun VoicePromptCardPreview() {
//    VoxAidTheme {
//        Surface(color = MaterialTheme.colorScheme.errorContainer) {
//            VoicePromptCard(
//                step = EmergencyStep.VoicePromptStep(
//                    stepId = "survey",
//                    promptText = "Survey the scene for safety.\n\nSay 'SAFE' when the area is secure.",
//                    expectedCommands = listOf("safe", "clear", "secure"),
//                    voicePrompt = "Survey the scene. Say Safe when secure.",
//                    nextStepId = "next",
//                    timeoutSeconds = 30
//                ),
//                isListening = true,
//                onManualContinue = {},
//                onRepeat = {}
//            )
//        }
//    }
//}