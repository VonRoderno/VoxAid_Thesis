package com.voxaid.core.design.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.voxaid.core.design.theme.VoxAidMicActive
import com.voxaid.core.design.theme.VoxAidMicInactive
import com.voxaid.core.design.theme.VoxAidTheme

/**
 * Microphone status indicator chip.
 * Shows visual feedback when voice input is active with pulsing animation.
 */
@Composable
fun MicChip(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val backgroundColor = if (isActive) VoxAidMicActive else VoxAidMicInactive
    val contentDescriptionText = if (isActive) {
        "Microphone is active and listening"
    } else {
        "Microphone is inactive"
    }

    Surface(
        modifier = modifier
            .size(40.dp)
            .then(
                if (isActive) {
                    Modifier
                        .scale(scale)
                        .alpha(alpha)
                } else {
                    Modifier
                }
            )
            .semantics {
                contentDescription = contentDescriptionText
            },
        shape = CircleShape,
        color = backgroundColor,
        shadowElevation = if (isActive) 4.dp else 0.dp
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.Mic else Icons.Default.MicOff,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MicChipPreview() {
    VoxAidTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MicChip(isActive = false)
            MicChip(isActive = true)
        }
    }
}