// feature/instruction/src/main/java/com/voxaid/feature/instruction/emergency/components/RescuerSwitchDialog.kt
package com.voxaid.feature.instruction.emergency.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.voxaid.core.design.theme.VoxAidTheme
import kotlinx.coroutines.delay

/**
 * Dialog asking if user is alone or with help.
 * Triggers at 2-minute mark of CPR compressions.
 *
 * Voice commands supported:
 * - "Yes" / "Someone here" → Switch rescuer flow
 * - "No" / "Alone" → Exhaustion check flow
 */
@Composable
fun RescuerSwitchDialog(
    onAlone: () -> Unit,
    onWithHelp: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.People,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Are you with someone?",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "You've been giving CPR for 2 minutes.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                            text = "🎤 Voice Commands",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Say \"Yes\" or \"No\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onWithHelp,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Yes, someone is here")
                }

                OutlinedButton(
                    onClick = onAlone,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("No, I'm alone")
                }
            }
        }
    )
}

/**
 * Auto-dismissing overlay shown during rescuer switch.
 * Displays 5-second countdown and returns to compressions.
 */
@Composable
fun SwitchingRescuerOverlay(
    onComplete: () -> Unit
) {
    var countdown by remember { mutableStateOf(5) }

    // Smooth pulsing animation for urgency
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            pulse.animateTo(1.1f, tween(600, easing = LinearEasing))
            pulse.animateTo(1f, tween(600, easing = LinearEasing))
        }
    }

    // Countdown logic
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        onComplete()
    }

    // Dimmed background overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .scale(pulse.value),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 28.dp, vertical = 32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 👥 Header icon
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .padding(bottom = 12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Switch Rescuers",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Change positions quickly.\nEnsure smooth transition.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Countdown circle with gradient glow
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                    CircularProgressIndicator(
                        progress = { (5 - countdown) / 5f },
                        modifier = Modifier.size(100.dp),
                        strokeWidth = 10.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$countdown",
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Resuming compressions...",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RescuerSwitchDialogPreview() {
    VoxAidTheme {
        RescuerSwitchDialog(
            onAlone = {},
            onWithHelp = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SwitchingRescuerOverlayPreview() {
    VoxAidTheme {
        SwitchingRescuerOverlay(onComplete = {})
    }
}