package com.voxaid.feature.instruction.components
//
//import android.annotation.SuppressLint
//import android.media.AudioAttributes
//import android.media.SoundPool
//import android.os.Build
//import android.os.VibrationEffect
//import android.os.Vibrator
//import android.os.VibratorManager
//import androidx.compose.animation.core.*
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Favorite
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.scale
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.semantics.contentDescription
//import androidx.compose.ui.semantics.semantics
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import com.voxaid.core.design.theme.VoxAidTheme
//import com.voxaid.feature.instruction.R
//import kotlinx.coroutines.android.awaitFrame
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.isActive
//import timber.log.Timber
//
///**
// * CPR metronome with synchronized audio, vibration, and animation.
// */
//@Composable
//fun Metronome(
//    modifier: Modifier = Modifier,
//    bpm: Int = 110,
//    isPlaying: Boolean = false,
//    onBeat: () -> Unit = {}
//) {
//    val context = LocalContext.current
//    val currentOnBeat by rememberUpdatedState(onBeat)
//
//    var beatCount by remember { mutableIntStateOf(0) }
//    var isBeating by remember { mutableStateOf(false) }
//    var soundLoaded by remember { mutableStateOf(false) }
//    var soundLoadError by remember { mutableStateOf<String?>(null) }
//
//    val intervalMs = remember(bpm) { (60000.0 / bpm).toLong() }
//
//    // ✅ Remember SoundPool across recompositions
//    val soundPool = rememberSoundPool()
//
//    // ✅ Load sound once
//    val soundId = remember {
//        try {
//            val id = soundPool.load(context, R.raw.metronome_110bpm, 1)
//            Timber.d("Attempting to load metronome sound (ID: $id)")
//            id
//        } catch (e: Exception) {
//            Timber.e(e, "Failed to load metronome sound resource")
//            soundLoadError = e.message
//            0
//        }
//    }
//
//    // ✅ Handle load completion
//    DisposableEffect(soundPool) {
//        val listener = SoundPool.OnLoadCompleteListener { _, sampleId, status ->
//            if (status == 0) {
//                soundLoaded = true
//                Timber.i("✓ Metronome sound loaded successfully (ID: $sampleId)")
//            } else {
//                soundLoadError = "Load failed (status: $status)"
//                Timber.e("✗ Failed to load metronome sound (status: $status)")
//            }
//        }
//        soundPool.setOnLoadCompleteListener(listener)
//
//        onDispose {
//            Timber.d("Metronome composable disposed (SoundPool kept alive)")
//            soundPool.setOnLoadCompleteListener(null)
//        }
//    }
//
//    // ✅ Vibrator setup
//    val vibrator = remember {
//        try {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
//                vibratorManager?.defaultVibrator
//            } else {
//                @Suppress("DEPRECATION")
//                context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
//            }
//        } catch (e: Exception) {
//            Timber.w(e, "Vibrator not available")
//            null
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    fun vibrateDevice() {
//        try {
//            vibrator?.let { vib ->
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    vib.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
//                } else {
//                    @Suppress("DEPRECATION")
//                    vib.vibrate(50)
//                }
//            }
//        } catch (e: Exception) {
//            Timber.w(e, "Vibration failed")
//        }
//    }
//
//    // ✅ Metronome timing loop
//    LaunchedEffect(isPlaying, bpm, soundLoaded) {
//        if (!isPlaying) {
//            Timber.d("Metronome stopped")
//            return@LaunchedEffect
//        }
//
//        // Wait for sound to load
//        var attempts = 0
//        while (isActive && !soundLoaded && soundLoadError == null && attempts < 30) {
//            Timber.d("Waiting for sound to load... attempt $attempts")
//            delay(100)
//            attempts++
//        }
//
//        if (soundLoadError != null || !soundLoaded) {
//            Timber.e("Sound failed to load or timeout: $soundLoadError")
//            return@LaunchedEffect
//        }
//
//        beatCount = 0
//        Timber.d("Metronome started at $bpm BPM")
//
//        var lastBeatTime = System.currentTimeMillis()
//
//        while (isActive && isPlaying) {
//            awaitFrame()
//            val now = System.currentTimeMillis()
//            if (now - lastBeatTime >= intervalMs) {
//                lastBeatTime = now
//                beatCount++
//                isBeating = true
//
//                try {
//                    currentOnBeat()
//                    Timber.d("Beat $beatCount: onBeat() called")
//                } catch (e: Exception) {
//                    Timber.e(e, "Error in onBeat callback")
//                }
//
//                // Play sound
//                if (soundId > 0) {
//                    val streamId = soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
//                    if (streamId == 0) {
//                        Timber.w("⚠️ Sound playback failed (streamId=0)")
//                    }
//                }
//
//                vibrateDevice()
//            }
//        }
//
//        Timber.d("Metronome loop exited")
//    }
//
//    // Reset beat animation
//    LaunchedEffect(beatCount) {
//        if (beatCount > 0) {
//            delay(100)
//            isBeating = false
//        }
//    }
//
//    val scale by animateFloatAsState(
//        targetValue = if (isBeating) 1.3f else 1f,
//        animationSpec = spring(
//            dampingRatio = Spring.DampingRatioMediumBouncy,
//            stiffness = Spring.StiffnessHigh
//        ),
//        label = "beat_scale"
//    )
//
//    Card(
//        modifier = modifier
//            .fillMaxWidth()
//            .semantics {
//                contentDescription = if (isPlaying)
//                    "Metronome active at $bpm BPM. Beat count: $beatCount"
//                else "Metronome stopped"
//            },
//        colors = CardDefaults.cardColors(
//            containerColor = if (isPlaying)
//                MaterialTheme.colorScheme.errorContainer
//            else MaterialTheme.colorScheme.surfaceVariant
//        ),
//        elevation = CardDefaults.cardElevation(
//            defaultElevation = if (isPlaying) 4.dp else 2.dp
//        )
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(24.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Box(
//                modifier = Modifier
//                    .size(80.dp)
//                    .scale(scale)
//                    .background(
//                        color = if (isPlaying)
//                            MaterialTheme.colorScheme.error
//                        else MaterialTheme.colorScheme.outline,
//                        shape = CircleShape
//                    ),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Favorite,
//                    contentDescription = null,
//                    modifier = Modifier.size(40.dp),
//                    tint = Color.White
//                )
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Text(
//                text = "$bpm BPM",
//                style = MaterialTheme.typography.headlineMedium,
//                fontWeight = FontWeight.Bold,
//                color = if (isPlaying)
//                    MaterialTheme.colorScheme.onErrorContainer
//                else MaterialTheme.colorScheme.onSurfaceVariant
//            )
//
//            if (isPlaying) {
//                Spacer(modifier = Modifier.height(8.dp))
//                Text(
//                    text = "Compressions: $beatCount",
//                    style = MaterialTheme.typography.titleMedium,
//                    color = MaterialTheme.colorScheme.onErrorContainer
//                )
//
//                Spacer(modifier = Modifier.height(8.dp))
//                when {
//                    soundLoadError != null -> Text(
//                        text = "⚠️ Sound load error",
//                        style = MaterialTheme.typography.bodySmall,
//                        color = MaterialTheme.colorScheme.error
//                    )
//                    !soundLoaded -> Row(
//                        horizontalArrangement = Arrangement.Center,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text(
//                            text = "Loading sound...",
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f)
//                        )
//                    }
//                    else -> Text(
//                        text = "Follow the beat rhythm",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
//                    )
//                }
//            } else {
//                Spacer(modifier = Modifier.height(8.dp))
//                Text(
//                    text = "Say \"Start Metronome\" to begin",
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//        }
//    }
//}
//
///**
// * Keeps SoundPool alive across recompositions and releases on app-level dispose.
// */
//@Composable
//fun rememberSoundPool(): SoundPool {
//    val soundPool = remember {
//        SoundPool.Builder()
//            .setMaxStreams(1)
//            .setAudioAttributes(
//                AudioAttributes.Builder()
//                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
//                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
//                    .build()
//            )
//            .build()
//    }
//
//    DisposableEffect(Unit) {
//        onDispose {
//            Timber.d("Releasing global SoundPool instance")
//            soundPool.release()
//        }
//    }
//
//    return soundPool
//}
//
//@Preview(showBackground = true)
//@Composable
//private fun MetronomePreview() {
//    VoxAidTheme {
//        Column(
//            modifier = Modifier.padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            Metronome(bpm = 110, isPlaying = false)
//            Metronome(bpm = 110, isPlaying = true)
//        }
//    }
//}