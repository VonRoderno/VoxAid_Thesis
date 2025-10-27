package com.voxaid.feature.instruction.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.voxaid.core.design.theme.VoxAidTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Auto-advance timer for emergency mode.
 * Shows countdown until automatic step advancement.
 * Can be cancelled by voice command or manual navigation.
 *
 * @param durationSeconds Total duration in seconds
 * @param isPaused Whether timer is paused
 * @param onComplete Callback when timer completes
 * @param onCancel Callback when timer is cancelled
 */
@Composable
fun AutoAdvanceTimer(
    durationSeconds: Int,
    isPaused: Boolean = false,
    onComplete: () -> Unit,
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var remainingSeconds by remember(durationSeconds) { mutableStateOf(durationSeconds) }
    var isCancelled by remember { mutableStateOf(false) }

    // Countdown logic
    LaunchedEffect(durationSeconds, isPaused) {
        if (isPaused || isCancelled) return@LaunchedEffect

        remainingSeconds = durationSeconds

        while (isActive && remainingSeconds > 0 && !isPaused && !isCancelled) {
            delay(1000)
            remainingSeconds--
        }

        if (remainingSeconds == 0 && !isCancelled) {
            onComplete()
        }
    }

    val progress = remember(remainingSeconds, durationSeconds) {
        remainingSeconds.toFloat() / durationSeconds
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "timer_progress"
    )

    if (!isCancelled && remainingSeconds > 0) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Auto-advancing in $remainingSeconds seconds"
                },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Advancing",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Text(
                            text = "${remainingSeconds}s remaining",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    TextButton(
                        onClick = {
                            isCancelled = true
                            onCancel()
                        }
                    ) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AutoAdvanceTimerPreview() {
    VoxAidTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AutoAdvanceTimer(
                durationSeconds = 10,
                onComplete = {},
                onCancel = {}
            )
        }
    }
}