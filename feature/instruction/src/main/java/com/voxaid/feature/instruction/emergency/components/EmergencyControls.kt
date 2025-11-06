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
    // Pulsing animation for voice indicator
    val infiniteTransition = rememberInfiniteTransition(label = "voice_pulse")
    val voicePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Color animation for voice indicator
    val voiceAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Voice hint bubble (animated entry/exit)
            AnimatedVisibility(
                visible = voiceHint != null,
                enter = slideInVertically { -it } + expandVertically() + fadeIn(),
                exit = slideOutVertically { -it } + shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(20.dp)
                                .scale(voicePulse)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = voiceHint ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button with scale animation
                var isBackPressed by remember { mutableStateOf(false) }
                val backScale by animateFloatAsState(
                    targetValue = if (isBackPressed) 0.9f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh
                    ),
                    label = "back_scale"
                )

                OutlinedIconButton(
                    onClick = {
                        isBackPressed = true
                        Timber.d("Back button clicked")
                        onBack()
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .scale(backScale),
                    colors = IconButtonDefaults.outlinedIconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back",
                        modifier = Modifier.size(28.dp)
                    )
                }

                LaunchedEffect(isBackPressed) {
                    if (isBackPressed) {
                        kotlinx.coroutines.delay(100)
                        isBackPressed = false
                    }
                }

                // Repeat button (primary action)
                var isRepeatPressed by remember { mutableStateOf(false) }
                val repeatScale by animateFloatAsState(
                    targetValue = if (isRepeatPressed) 0.95f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh
                    ),
                    label = "repeat_scale"
                )

                FilledTonalButton(
                    onClick = {
                        isRepeatPressed = true
                        Timber.d("Repeat button clicked")
                        onRepeat()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .scale(repeatScale),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "REPEAT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                LaunchedEffect(isRepeatPressed) {
                    if (isRepeatPressed) {
                        kotlinx.coroutines.delay(100)
                        isRepeatPressed = false
                    }
                }

                // Next button with scale animation
                var isNextPressed by remember { mutableStateOf(false) }
                val nextScale by animateFloatAsState(
                    targetValue = if (isNextPressed) 0.9f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh
                    ),
                    label = "next_scale"
                )

                Button(
                    onClick = {
                        isNextPressed = true
                        Timber.d("Next button clicked")
                        onNext()
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .scale(nextScale),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next step",
                        modifier = Modifier.size(28.dp)
                    )
                }

                LaunchedEffect(isNextPressed) {
                    if (isNextPressed) {
                        kotlinx.coroutines.delay(100)
                        isNextPressed = false
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status bar with voice indicator and micro-hints
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(if (isListening) voicePulse else 1f)
                            .clip(CircleShape)
                            .background(
                                if (isListening) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = voiceAlpha)
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                }
                            )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = null,
                        tint = if (isListening) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = if (isListening) "Voice Active" else "Voice Inactive",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isListening) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                }

                // Quick reference hint
                Text(
                    text = "👆 Tap • 👈👉 Swipe • 🎤 Speak",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.End
                )
            }
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