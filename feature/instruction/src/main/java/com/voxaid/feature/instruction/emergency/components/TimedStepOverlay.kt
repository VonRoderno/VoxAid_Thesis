package com.voxaid.feature.instruction.emergency.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.voxaid.core.design.theme.VoxAidTheme

/**
 * Overlay component for timed emergency steps.
 * Shows countdown timer with pulsing animation.
 */
@Composable
fun TimedStepOverlay(
    elapsedSeconds: Int,
    totalSeconds: Int,
    beatCount: Int? = null,
    targetBeats: Int? = null,
    modifier: Modifier = Modifier
) {
    val progress = elapsedSeconds.toFloat() / totalSeconds.toFloat()

    val infiniteTransition = rememberInfiniteTransition(label = "timer_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Timer or beat counter
            if (beatCount != null && targetBeats != null) {
                // Beat counter mode
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale)
                        .background(
                            color = MaterialTheme.colorScheme.error,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$beatCount",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onError
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "of $targetBeats compressions",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            } else {
                // Time countdown mode
                val remainingSeconds = totalSeconds - elapsedSeconds
                val minutes = remainingSeconds / 60
                val seconds = remainingSeconds % 60

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale)
                        .background(
                            color = MaterialTheme.colorScheme.error,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onError
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "remaining",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = MaterialTheme.colorScheme.error,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimedStepOverlayPreview() {
    VoxAidTheme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TimedStepOverlay(
                elapsedSeconds = 45,
                totalSeconds = 120
            )

            TimedStepOverlay(
                elapsedSeconds = 0,
                totalSeconds = 30,
                beatCount = 15,
                targetBeats = 30
            )
        }
    }
}