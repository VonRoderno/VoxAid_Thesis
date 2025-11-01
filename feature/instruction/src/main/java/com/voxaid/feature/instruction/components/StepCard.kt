package com.voxaid.feature.instruction.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.voxaid.core.content.model.Step
import com.voxaid.core.design.theme.VoxAidTheme

/**
 * Card displaying a single instruction step.
 * Adapts styling for emergency vs instructional mode.
 * Now uses GIF animations instead of Lottie.
 */
@Composable
fun StepCard(
    step: Step,
    isEmergencyMode: Boolean,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEmergencyMode) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEmergencyMode) 0.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Step number indicator
            Text(
                text = "Step ${step.stepNumber} of $totalSteps",
                style = if (isEmergencyMode) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.titleMedium
                },
                color = if (isEmergencyMode) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.primary
                },
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(if (isEmergencyMode) 24.dp else 16.dp))

            // Step title
            Text(
                text = step.title,
                style = if (isEmergencyMode) {
                    MaterialTheme.typography.displaySmall
                } else {
                    MaterialTheme.typography.headlineMedium
                },
                color = if (isEmergencyMode) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(if (isEmergencyMode) 16.dp else 12.dp))

            // Critical warning if present
            step.criticalWarning?.let { warning ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⚠️ $warning",
                        style = if (isEmergencyMode) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        color = MaterialTheme.colorScheme.onError,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Main description
            Text(
                text = step.description,
                style = if (isEmergencyMode) {
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                color = if (isEmergencyMode) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                lineHeight = if (isEmergencyMode) {
                    MaterialTheme.typography.headlineSmall.lineHeight.times(1.3f)
                } else {
                    MaterialTheme.typography.bodyLarge.lineHeight.times(1.5f)
                }
            )

            // Detailed instructions
            if (step.detailedInstructions.isNotEmpty() && !isEmergencyMode) {
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Detailed Steps:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        step.detailedInstructions.forEachIndexed { index, instruction ->
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${index + 1}. ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = instruction,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // GIF Animation (replaces Lottie)
            step.animationResource?.let { animationRes ->
                Spacer(modifier = Modifier.height(24.dp))

                AnimationView(
                    animationResource = animationRes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isEmergencyMode) 280.dp else 250.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StepCardPreview() {
    VoxAidTheme {
        StepCard(
            step = Step(
                stepNumber = 1,
                title = "Check Responsiveness",
                description = "Tap the person's shoulders and shout 'Are you okay?'",
                detailedInstructions = listOf(
                    "Gently shake the person's shoulders",
                    "Speak loudly and clearly",
                    "Look for any response or movement"
                ),
                voicePrompt = "Check if the person responds.",
                animationResource = "cpr_check_response.json",
                criticalWarning = "Do not move if spinal injury suspected"
            ),
            isEmergencyMode = false,
            totalSteps = 6
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StepCardEmergencyPreview() {
    VoxAidTheme {
        StepCard(
            step = Step(
                stepNumber = 4,
                title = "Start Compressions",
                description = "Push hard and fast, 100-120 per minute",
                voicePrompt = "Start chest compressions.",
                criticalWarning = "Compressions save lives. Don't stop!",
                animationResource = "cpr_compressions.json"
            ),
            isEmergencyMode = true,
            totalSteps = 6
        )
    }
}