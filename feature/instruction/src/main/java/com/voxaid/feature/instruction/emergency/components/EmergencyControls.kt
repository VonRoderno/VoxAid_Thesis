package com.voxaid.feature.instruction.emergency.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.voxaid.core.design.theme.VoxAidTheme
import timber.log.Timber

/**
 * Enhanced emergency mode control panel with voice state visualization.
 *
 * Phase 2 Improvements:
 * - Animated voice state indicator
 * - Quick reference command hints
 * - Enhanced button feedback animations
 * - Accessibility announcements
 */
@Composable
fun EmergencyControls(
    onBack: () -> Unit,
    onRepeat: () -> Unit,
    onNext: () -> Unit,
    voiceHint: String? = null,
    isListening: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_anim")

    // Pulse for mic and voice ring
    val voicePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "voice_pulse"
    )

    // Glow opacity
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "voice_glow"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp)),
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // 🔹 Voice hint bubble with subtle glow
            AnimatedVisibility(
                visible = voiceHint != null,
                enter = fadeIn(tween(300)) + expandVertically(),
                exit = fadeOut(tween(300)) + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .scale(if (isListening) voicePulse else 1f)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = glowAlpha)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = voiceHint.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // 🔸 Control Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ⬅️ Back
                AnimatedButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    tint = MaterialTheme.colorScheme.onSurface,
                    borderColor = MaterialTheme.colorScheme.outline,
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    onClick = onBack
                )

                // 🔁 Repeat (primary action)
                Button(
                    onClick = onRepeat,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "REPEAT",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // ➡️ Next (emergency emphasis)
                AnimatedButton(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    tint = MaterialTheme.colorScheme.onError,
                    backgroundColor = MaterialTheme.colorScheme.error,
                    onClick = onNext
                )
            }

            // 🔊 Voice Status Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(if (isListening) voicePulse else 1f)
                            .clip(CircleShape)
                            .background(
                                if (isListening)
                                    MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha)
                                else
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isListening) "Listening..." else "Voice Inactive",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isListening)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

            }
        }
    }
}

@Composable
private fun AnimatedButton(
    icon: ImageVector,
    tint: Color,
    backgroundColor: Color,
    borderColor: Color = Color.Transparent,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "button_scale"
    )

    OutlinedIconButton(
        onClick = {
            pressed = true
            onClick()
        },
        modifier = Modifier
            .size(56.dp)
            .scale(scale),
        colors = IconButtonDefaults.outlinedIconButtonColors(
            containerColor = backgroundColor,
            contentColor = tint
        ),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
    }

    LaunchedEffect(pressed) {
        if (pressed) {
            kotlinx.coroutines.delay(100)
            pressed = false
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmergencyControlsPreview() {
    VoxAidTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Active voice with all buttons
            EmergencyControls(
                onBack = {},
                onRepeat = {},
                onNext = {},
                voiceHint = null,
                isListening = true
            )

            // With voice hint
            EmergencyControls(
                onBack = {},
                onRepeat = {},
                onNext = {},
                voiceHint = "Try saying: SAFE, CLEAR, or OKAY",
                isListening = true
            )

            // Waiting for voice input
            EmergencyControls(
                onBack = {},
                onRepeat = {},
                onNext = {},
                voiceHint = "Say YES or NO to continue",
                isListening = true
            )

            // Voice inactive
            EmergencyControls(
                onBack = {},
                onRepeat = {},
                onNext = {},
                voiceHint = null,
                isListening = false
            )
        }
    }
}