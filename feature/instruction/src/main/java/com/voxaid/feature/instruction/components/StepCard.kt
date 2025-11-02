package com.voxaid.feature.instruction.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.voxaid.core.content.model.Step
import com.voxaid.core.design.theme.VoxAidTheme

/**
 * Card displaying a single instruction step.
 * Adapts styling for emergency vs instructional mode.
 *
 * Updated: Removed detailed instructions list for Learning Mode.
 * Tips and notes are now included in the critical_warning field.
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

            // Critical warning/Tips (displayed for both modes)
            // Critical warning/Tips (displayed for both modes)
            step.criticalWarning?.let { warning ->
                Spacer(modifier = Modifier.height(16.dp))

                val borderColor = if (warning.startsWith("Tip:"))
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(2.dp, borderColor),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (warning.startsWith("Tip:")) "💡" else "⚠️",
                            style = if (isEmergencyMode) MaterialTheme.typography.titleLarge
                            else MaterialTheme.typography.titleMedium,
                            color = borderColor
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = warning.removePrefix("Tip:").trim(),
                            style = if (isEmergencyMode) MaterialTheme.typography.titleMedium
                            else MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (warning.startsWith("Tip:")) FontWeight.Medium else FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // GIF Animation
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
                title = "Survey the Scene",
                description = "Tap the shoulder of the patient and shout 'Are you okay?' to see if they are responding.",
                voicePrompt = "Survey the scene. Tap the patient's shoulder and shout, Are you okay?",
                animationResource = "cpr_check_response.json",
                criticalWarning = "Check if patient is conscious, breathing, and has a pulse. Only perform CPR for patients who are unconscious, not breathing, and have no pulse."
            ),
            isEmergencyMode = false,
            totalSteps = 9
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StepCardWithTipPreview() {
    VoxAidTheme {
        StepCard(
            step = Step(
                stepNumber = 5,
                title = "Position Your Hands",
                description = "Make sure both your hands are locked on top of each other. Your shoulders should be directly over your hands and your elbows are locked.",
                voicePrompt = "Lock both hands on top of each other.",
                animationResource = "cpr_hand_position.json",
                criticalWarning = "Tip: Your dominant hand should be on the bottom"
            ),
            isEmergencyMode = false,
            totalSteps = 9
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StepCardEmergencyPreview() {
    VoxAidTheme {
        StepCard(
            step = Step(
                stepNumber = 6,
                title = "Perform 30 Chest Compressions",
                description = "Perform chest compressions with a rate of 100 to 120 per minute and count up until 30.",
                voicePrompt = "Start chest compressions. Count to 30.",
                criticalWarning = "Tip: Allow the chest to fully recoil or get back to its natural position between compressions",
                animationResource = "cpr_compressions.json"
            ),
            isEmergencyMode = true,
            totalSteps = 9
        )
    }
}