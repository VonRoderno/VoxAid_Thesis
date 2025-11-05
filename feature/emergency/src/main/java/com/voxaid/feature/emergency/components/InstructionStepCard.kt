//package com.voxaid.feature.emergency.components
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowForward
//import androidx.compose.material.icons.filled.Refresh
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import com.voxaid.core.content.model.EmergencyStep
//import com.voxaid.core.design.components.GifImage
//import com.voxaid.core.design.theme.VoxAidTheme
//
///**
// * Card for displaying instruction steps in Emergency Mode.
// * Large text, clear visuals, simple navigation.
// */
//@Composable
//fun InstructionStepCard(
//    step: EmergencyStep.InstructionStep,
//    elapsedSeconds: Int,
//    onContinue: () -> Unit,
//    onRepeat: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    Column(
//        modifier = modifier
//            .fillMaxSize()
//            .padding(24.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.SpaceBetween
//    ) {
//        Column(
//            modifier = Modifier.weight(1f),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center
//        ) {
//            // Title
//            Text(
//                text = step.title,
//                style = MaterialTheme.typography.displayMedium,
//                fontWeight = FontWeight.Bold,
//                color = MaterialTheme.colorScheme.onErrorContainer,
//                textAlign = TextAlign.Center
//            )
//
//            Spacer(modifier = Modifier.height(24.dp))
//
//            // Description
//            Text(
//                text = step.description,
//                style = MaterialTheme.typography.headlineSmall,
//                color = MaterialTheme.colorScheme.onErrorContainer,
//                textAlign = TextAlign.Center,
//                lineHeight = MaterialTheme.typography.headlineSmall.lineHeight.times(1.3f)
//            )
//
//            Spacer(modifier = Modifier.height(32.dp))
//
//            // Animation
//            step.animationResource?.let { animRes ->
//                GifImage(
//                    resourceId = animRes,
//                    contentDescription = step.title,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(280.dp),
//                    showPlaceholder = true
//                )
//
//                Spacer(modifier = Modifier.height(24.dp))
//            }
//
//            // Critical warning
//            step.criticalWarning?.let { warning ->
//                Card(
//                    colors = CardDefaults.cardColors(
//                        containerColor = if (warning.startsWith("Tip:")) {
//                            MaterialTheme.colorScheme.primaryContainer
//                        } else {
//                            MaterialTheme.colorScheme.error
//                        }
//                    ),
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    Row(
//                        modifier = Modifier.padding(16.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            text = if (warning.startsWith("Tip:")) "💡" else "⚠️",
//                            style = MaterialTheme.typography.headlineMedium
//                        )
//                        Spacer(modifier = Modifier.width(12.dp))
//                        Text(
//                            text = warning,
//                            style = MaterialTheme.typography.titleMedium,
//                            fontWeight = FontWeight.Bold,
//                            color = if (warning.startsWith("Tip:")) {
//                                MaterialTheme.colorScheme.onPrimaryContainer
//                            } else {
//                                MaterialTheme.colorScheme.onError
//                            }
//                        )
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//            }
//        }
//
//        // Controls
//        Column(
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            // Auto-advance indicator
//            step.durationSeconds?.let { duration ->
//                val remaining = maxOf(0, duration - (elapsedSeconds % duration))
//
//                if (remaining > 0) {
//                    LinearProgressIndicator(
//                        progress = { 1f - (remaining.toFloat() / duration) },
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(8.dp),
//                        color = MaterialTheme.colorScheme.error
//                    )
//
//                    Spacer(modifier = Modifier.height(8.dp))
//
//                    Text(
//                        text = "Auto-continues in ${remaining}s",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
//                        modifier = Modifier.fillMaxWidth(),
//                        textAlign = TextAlign.Center
//                    )
//
//                    Spacer(modifier = Modifier.height(16.dp))
//                }
//            }
//
//            // Buttons
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.spacedBy(12.dp)
//            ) {
//                OutlinedButton(
//                    onClick = onRepeat,
//                    modifier = Modifier.weight(1f),
//                    colors = ButtonDefaults.outlinedButtonColors(
//                        contentColor = MaterialTheme.colorScheme.onErrorContainer
//                    )
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Refresh,
//                        contentDescription = null
//                    )
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text("Repeat")
//                }
//
//                Button(
//                    onClick = onContinue,
//                    modifier = Modifier.weight(1f),
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = MaterialTheme.colorScheme.error
//                    )
//                ) {
//                    Text("Continue", style = MaterialTheme.typography.titleMedium)
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Icon(
//                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
//                        contentDescription = null
//                    )
//                }
//            }
//        }
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//private fun InstructionStepCardPreview() {
//    VoxAidTheme {
//        Surface(color = MaterialTheme.colorScheme.errorContainer) {
//            InstructionStepCard(
//                step = EmergencyStep.InstructionStep(
//                    stepId = "position",
//                    title = "Position Patient",
//                    description = "Ensure the patient is lying on their back on a hard, flat surface.\n\nMove them if necessary for safety and effective compressions.",
//                    voicePrompt = "Position patient on flat surface.",
//                    nextStepId = "next",
//                    durationSeconds = 10,
//                    criticalWarning = "Patient must be on a firm surface for effective compressions"
//                ),
//                elapsedSeconds = 3,
//                onContinue = {},
//                onRepeat = {}
//            )
//        }
//    }
//}