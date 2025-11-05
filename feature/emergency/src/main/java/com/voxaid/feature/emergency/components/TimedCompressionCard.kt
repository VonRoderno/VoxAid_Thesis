//package com.voxaid.feature.emergency.components
//
//import androidx.compose.animation.core.*
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Favorite
//import androidx.compose.material.icons.filled.Refresh
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.scale
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import com.voxaid.core.content.model.EmergencyStep
//import com.voxaid.core.design.theme.VoxAidTheme
//
///**
// * Timed compression card with metronome visualization.
// * Shows large compression count and pulsing beat indicator.
// *
// * Used for: CPR chest compressions (count to 30)
// */
//@Composable
//fun TimedCompressionCard(
//    step: EmergencyStep.TimedActionStep,
//    currentCount: Int,
//    isMetronomeActive: Boolean,
//    metronomeBpm: Int,
//    onRepeat: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    // Pulse animation for metronome beat
//    val infiniteTransition = rememberInfiniteTransition(label = "metronome_pulse")
//    val beat by infiniteTransition.animateFloat(
//        initialValue = 1f,
//        targetValue = 1.3f,
//        animationSpec = infiniteRepeatable(
//            animation = tween(
//                durationMillis = (60000 / metronomeBpm) / 2, // Half beat duration
//                easing = FastOutSlowInEasing
//            ),
//            repeatMode = RepeatMode.Reverse
//        ),
//        label = "beat"
//    )
//
//    Column(
//        modifier = modifier
//            .fillMaxSize()
//            .padding(24.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.SpaceBetween
//    ) {
//        Column(
//            modifier = Modifier.weight(1f),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center
//        ) {
//            // Title
//            Text(
//                text = step.title,
//                style = MaterialTheme.typography.displaySmall,
//                fontWeight = FontWeight.Bold,
//                color = MaterialTheme.colorScheme.onErrorContainer,
//                textAlign = TextAlign.Center
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Description
//            Text(
//                text = step.description,
//                style = MaterialTheme.typography.titleLarge,
//                color = MaterialTheme.colorScheme.onErrorContainer,
//                textAlign = TextAlign.Center,
//                lineHeight = MaterialTheme.typography.titleLarge.lineHeight.times(1.3f)
//            )
//
//            Spacer(modifier = Modifier.height(48.dp))
//
//            // Metronome beat indicator
//            Box(
//                modifier = Modifier
//                    .size(160.dp)
//                    .scale(if (isMetronomeActive) beat else 1f)
//                    .background(
//                        color = if (isMetronomeActive) {
//                            MaterialTheme.colorScheme.error
//                        } else {
//                            MaterialTheme.colorScheme.surfaceVariant
//                        },
//                        shape = CircleShape
//                    ),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Favorite,
//                    contentDescription = null,
//                    modifier = Modifier.size(80.dp),
//                    tint = Color.White
//                )
//            }
//
//            Spacer(modifier = Modifier.height(32.dp))
//
//            // Count display
//            Card(
//                colors = CardDefaults.cardColors(
//                    containerColor = MaterialTheme.colorScheme.error
//                ),
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                Column(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(24.dp),
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    Text(
//                        text = "$currentCount",
//                        style = MaterialTheme.typography.displayLarge.copy(
//                            fontSize = MaterialTheme.typography.displayLarge.fontSize.times(1.5f)
//                        ),
//                        fontWeight = FontWeight.Bold,
//                        color = MaterialTheme.colorScheme.onError
//                    )
//
//                    Text(
//                        text = "of ${step.targetCount}",
//                        style = MaterialTheme.typography.headlineMedium,
//                        color = MaterialTheme.colorScheme.onError.copy(alpha = 0.8f)
//                    )
//                }
//            }
//
//            Spacer(modifier = Modifier.height(24.dp))
//
//            // Progress bar
//            LinearProgressIndicator(
//                progress = { currentCount.toFloat() / step.targetCount },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(12.dp),
//                color = MaterialTheme.colorScheme.primary,
//                trackColor = MaterialTheme.colorScheme.surfaceVariant
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // BPM indicator
//            Card(
//                colors = CardDefaults.cardColors(
//                    containerColor = MaterialTheme.colorScheme.primaryContainer
//                )
//            ) {
//                Row(
//                    modifier = Modifier.padding(16.dp),
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Icon(
//                        imageVector = Icons.Default.Favorite,
//                        contentDescription = null,
//                        modifier = Modifier.size(24.dp),
//                        tint = MaterialTheme.colorScheme.onPrimaryContainer
//                    )
//                    Spacer(modifier = Modifier.width(12.dp))
//                    Text(
//                        text = "$metronomeBpm BPM - Follow the beat rhythm",
//                        style = MaterialTheme.typography.titleMedium,
//                        fontWeight = FontWeight.SemiBold,
//                        color = MaterialTheme.colorScheme.onPrimaryContainer
//                    )
//                }
//            }
//        }
//
//        // Controls
//        OutlinedButton(
//            onClick = onRepeat,
//            modifier = Modifier.fillMaxWidth(),
//            colors = ButtonDefaults.outlinedButtonColors(
//                contentColor = MaterialTheme.colorScheme.onErrorContainer
//            )
//        ) {
//            Icon(
//                imageVector = Icons.Default.Refresh,
//                contentDescription = null
//            )
//            Spacer(modifier = Modifier.width(8.dp))
//            Text("Repeat Instructions")
//        }
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//private fun TimedCompressionCardPreview() {
//    VoxAidTheme {
//        Surface(color = MaterialTheme.colorScheme.errorContainer) {
//            TimedCompressionCard(
//                step = EmergencyStep.TimedActionStep(
//                    stepId = "compressions",
//                    title = "Chest Compressions",
//                    description = "Push hard and fast.\n\nCompress at least 2 inches deep.\nFollow the metronome beat.\n\nCount to 30 compressions.",
//                    voicePrompt = "Start chest compressions.",
//                    targetCount = 30,
//                    metronomeBpm = 110,
//                    nextStepId = "next"
//                ),
//                currentCount = 12,
//                isMetronomeActive = true,
//                metronomeBpm = 110,
//                onRepeat = {}
//            )
//        }
//    }
//}