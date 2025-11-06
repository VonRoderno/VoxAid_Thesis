package com.voxaid.feature.instruction.components

import android.view.Choreographer
import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.delay
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
import com.voxaid.feature.instruction.R
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.isActive
import timber.log.Timber

/**
 * CPR metronome component with precise BPM timing.
 * Uses Choreographer for frame-accurate timing.
 *
 * @param bpm Beats per minute (recommended: 100-120 for CPR)
 * @param isPlaying Whether the metronome is active
 * @param onBeat Callback invoked on each beat
 */
@Composable
fun Metronome(
    modifier: Modifier = Modifier,
    bpm: Int = 110,
    isPlaying: Boolean = false,
    onBeat: () -> Unit = {},

) {
    val context = LocalContext.current
    var beatCount by remember { mutableStateOf(0) }
    var isBeating by remember { mutableStateOf(false) }
    var soundLoaded by remember { mutableStateOf(false) }

    // Calculate interval in milliseconds
    val intervalMs = remember(bpm) {
        (60000.0 / bpm).toLong()
    }
    // --- SoundPool setup ---
    val soundPool = remember {
        SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
    }

    // Load tick sound
    val soundId by remember {
        mutableIntStateOf(
            soundPool.load(context, R.raw.metronome_110bpm, 1)
        )
    }

    // Release SoundPool when no longer needed
    DisposableEffect(Unit) {
        val listener = SoundPool.OnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) soundLoaded = true
        }
        soundPool.setOnLoadCompleteListener(listener)
        onDispose {
            soundPool.setOnLoadCompleteListener(null)
        }
    }
    // Precise timing using Choreographer
    LaunchedEffect(isPlaying, bpm) {
        if (!isPlaying) {
            beatCount = 0
            return@LaunchedEffect
        }

        var lastBeatTime = System.currentTimeMillis()

        while (isActive && isPlaying) {
            awaitFrame()

            val currentTime = System.currentTimeMillis()
            val elapsed = currentTime - lastBeatTime

            if (elapsed >= intervalMs && soundLoaded) {
                beatCount++
                isBeating = true
                onBeat()

                soundPool.play(
                    soundId,
                    1f, // left volume
                    1f, // right volume
                    1,  // priority
                    0,  // no loop
                    1f  // normal playback rate
                )
                // Vibrate device for haptic feedback
                try {
                    val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE)
                            as? android.os.Vibrator
                    vibrator?.vibrate(50)
                } catch (e: Exception) {
                    Timber.w(e, "Vibration not available")
                }

                lastBeatTime = currentTime

                Timber.d("Metronome beat: $beatCount at $bpm BPM")
            }
        }
    }

    // Reset beat visual after animation
    LaunchedEffect(beatCount) {
        if (beatCount > 0) {
            kotlinx.coroutines.delay(100)
            isBeating = false
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isBeating) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "beat_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = if (isPlaying) {
                    "Metronome active at $bpm beats per minute. Beat count: $beatCount"
                } else {
                    "Metronome stopped"
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPlaying) 4.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Visual beat indicator
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale)
                    .background(
                        color = if (isPlaying) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BPM display
            Text(
                text = "$bpm BPM",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (isPlaying) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            if (isPlaying) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Compressions: $beatCount",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Follow the beat rhythm",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Say \"Start Metronome\" to begin",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MetronomePreview() {
    VoxAidTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Metronome(
                bpm = 110,
                isPlaying = false
            )

            Metronome(
                bpm = 110,
                isPlaying = true
            )
        }
    }
}