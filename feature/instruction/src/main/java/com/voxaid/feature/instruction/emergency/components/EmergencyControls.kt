package com.voxaid.feature.instruction.emergency.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import timber.log.Timber
import com.voxaid.core.design.theme.VoxAidTheme

/**
 * Emergency mode control buttons.
 * Provides manual navigation fallback when voice recognition fails.
 *
 * Features:
 * - Large, touch-friendly buttons
 * - Clear visual feedback
 * - Voice command hints
 * - Back/Repeat/Next controls
 */
@Composable
fun EmergencyControls(
    onBack: () -> Unit,
    onRepeat: () -> Unit,
    onNext: () -> Unit,
    showNext: Boolean = true,
    showBack: Boolean = false,
    voiceHint: String? = null,
    modifier: Modifier = Modifier
) {
    // Log button visibility for debugging
    Timber.d("EmergencyControls rendering - showNext: $showNext, showBack: $showBack")

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.errorContainer,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Voice hint (if voice recognition failed)
            voiceHint?.let { hint ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Control buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                if (showBack) {
                    OutlinedButton(
                        onClick = {
                            Timber.d("Back button clicked")
                            onBack()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Repeat button (always shown)
                Button(
                    onClick = {
                        Timber.d("Repeat button clicked")
                        onRepeat()
                    },
                    modifier = Modifier
                        .weight(if (showNext || showBack) 2f else 1f)
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "REPEAT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Next button
                if (showNext) {
                    Button(
                        onClick = {
                            Timber.d("Next button clicked")
                            onNext()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status text - show what's expected
            val statusText = when {
                !showNext && voiceHint != null -> "🎤 Waiting for voice: $voiceHint"
                showNext -> "🎤 Voice active  •  👆 Tap NEXT or swipe to continue"
                else -> "🎤 Voice commands active  •  Use buttons if voice fails"
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}