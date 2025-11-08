package com.voxaid.feature.instruction.components

import android.annotation.SuppressLint
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.voxaid.core.design.theme.VoxAidTheme
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import timber.log.Timber

/**
 * FALLBACK VERSION: Uses ToneGenerator instead of WAV file.
 * Use this if you're having trouble with the sound file.
 */
@Composable
fun MetronomeWithTone(
    modifier: Modifier = Modifier,
    bpm: Int = 110,
    isPlaying: Boolean = false,
    onBeat: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentOnBeat by rememberUpdatedState(onBeat)

    var beatCount by remember { mutableIntStateOf(0) }
    var isBeating by remember { mutableStateOf(false) }

    val intervalMs = remember(bpm) { (60000.0 / bpm).toLong() }

    // ToneGenerator setup
    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: Exception) {
            Timber.e(e, "ToneGenerator init failed")
            null
        }
    }

    // Vibrator setup
    val vibrator = remember {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            Timber.w(e, "Vibrator not available")
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            toneGenerator?.release()
            Timber.d("ToneGenerator released")
        }
    }

    @SuppressLint("MissingPermission")
    fun vibrateDevice() {
        try {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(
                        VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(40)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Vibration failed")
        }
    }

    // Metronome loop
    LaunchedEffect(isPlaying, bpm) {
        if (isPlaying) beatCount = 0 else return@LaunchedEffect

        var lastBeatTime = System.currentTimeMillis()

        while (isActive && isPlaying) {
            awaitFrame()
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBeatTime >= intervalMs) {
                beatCount++
                isBeating = true
                try {
                    currentOnBeat()
                } catch (e: Exception) {
                    Timber.e(e, "Error in onBeat callback")
                }
                try {
                    toneGenerator?.startTone(ToneGenerator.TONE_DTMF_S, 30)
                } catch (e: Exception) {
                    Timber.e(e, "Tone playback failed")
                }
                vibrateDevice()
                lastBeatTime = currentTime
            }
        }
    }

    LaunchedEffect(beatCount) {
        if (beatCount > 0) {
            delay(90)
            isBeating = false
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isBeating) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "beat_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics {
                contentDescription = if (isPlaying) {
                    "Metronome active at $bpm beats per minute. Beat count: $beatCount"
                } else {
                    "Metronome stopped"
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp) // smaller circle
                    .scale(scale)
                    .background(
                        color = if (isPlaying)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp), // smaller icon
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$bpm BPM",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isPlaying)
                    MaterialTheme.colorScheme.onErrorContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isPlaying) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Compressions: $beatCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Text(
                    text = if (toneGenerator != null) "✓ System tone active"
                    else "⚠️ Sound unavailable",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MetronomeWithTonePreview() {
    VoxAidTheme {
        MetronomeWithTone(isPlaying = true)
    }
}