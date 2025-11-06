// feature/instruction/src/main/java/com/voxaid/feature/instruction/emergency/components/TimerDisplay.kt
package com.voxaid.feature.instruction.emergency.components

import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.voxaid.core.design.theme.VoxAidTheme

/**
 * Timer display for CPR compression cycles.
 * Shows elapsed time with warning states at key intervals.
 *
 * Design principles:
 * - Calm, unobtrusive positioning (top-right corner)
 * - Visual warning at 1:40 (fade-in amber background)
 * - Smooth transitions to avoid startling users
 * - Always readable regardless of background
 */
@Composable
fun TimerDisplay(
    elapsedSeconds: Int,
    modifier: Modifier = Modifier,
    showWarning: Boolean = false
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60

    // Warning pulse animation at 1:40
    val infiniteTransition = rememberInfiniteTransition(label = "warning_pulse")
    val warningAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Background color transition
    val backgroundColor = if (showWarning) {
        MaterialTheme.colorScheme.tertiary.copy(alpha = warningAlpha)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        shadowElevation = if (showWarning) 4.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = if (showWarning) {
                    MaterialTheme.colorScheme.onTertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = String.format("%d:%02d", minutes, seconds),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (showWarning) {
                    MaterialTheme.colorScheme.onTertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            if (showWarning) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.alpha(warningAlpha)
                )
            }
        }
    }
}

/**
 * Warning banner shown at 1:40 mark.
 * Fades in smoothly below timer.
 */
@Composable
fun SwitchWarningBanner(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠️",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Prepare to switch soon",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerDisplayPreview() {
    VoxAidTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TimerDisplay(elapsedSeconds = 45)
            TimerDisplay(elapsedSeconds = 105, showWarning = true)

            SwitchWarningBanner(visible = true)
        }
    }
}