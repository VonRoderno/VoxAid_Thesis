package com.voxaid.feature.instruction.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.voxaid.core.design.theme.VoxAidTheme

/**
 * Navigation controls for instruction steps.
 * Adapts button sizes and styling for emergency mode.
 */
@Composable
fun StepControls(
    currentStepIndex: Int,
    totalSteps: Int,
    isEmergencyMode: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onRepeatClick: () -> Unit,
    modifier: Modifier = Modifier

) {
    val buttonHeight = if (isEmergencyMode) 64.dp else 56.dp
    val iconSize = if (isEmergencyMode) 32.dp else 24.dp

    Surface(
        modifier = modifier,
        color = if (isEmergencyMode) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { (currentStepIndex + 1).toFloat() / totalSteps },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (isEmergencyMode) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                trackColor = if (isEmergencyMode) {
                    MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button
                OutlinedButton(
                    onClick = onPreviousClick,
                    enabled = currentStepIndex > 0,
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight)
                        .semantics {
                            contentDescription = "Go to previous step"
                        },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isEmergencyMode) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(iconSize)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Back",
                        style = if (isEmergencyMode) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.labelLarge
                        }
                    )
                }

                // Repeat button
                if (isEmergencyMode) {
                    Button(
                        onClick = onRepeatClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(buttonHeight)
                            .semantics {
                                contentDescription = "Repeat current step"
                            },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Repeat",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onRepeatClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(buttonHeight)
                            .semantics {
                                contentDescription = "Repeat current step"
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(iconSize)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Repeat")
                    }
                }

                // Next button
                Button(
                    onClick = onNextClick,
                    enabled = currentStepIndex <= totalSteps - 1,
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight)
                        .semantics {
                            contentDescription = "Go to next step"
                        },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEmergencyMode) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Text(
                        text = if (currentStepIndex == totalSteps - 1) "Done" else "Next",
                        style = if (isEmergencyMode) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.labelLarge
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }

            // Voice command hint
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Say \"Next\", \"Back\", or \"Repeat\" to navigate",
                style = MaterialTheme.typography.bodySmall,
                color = if (isEmergencyMode) {
                    MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StepControlsPreview() {
    VoxAidTheme {
        Column {
            StepControls(
                currentStepIndex = 2,
                totalSteps = 6,
                isEmergencyMode = false,
                onPreviousClick = {},
                onNextClick = {},
                onRepeatClick = {}
            )

            Spacer(modifier = Modifier.height(16.dp))

            StepControls(
                currentStepIndex = 2,
                totalSteps = 6,
                isEmergencyMode = true,
                onPreviousClick = {},
                onNextClick = {},
                onRepeatClick = {}
            )
        }
    }
}