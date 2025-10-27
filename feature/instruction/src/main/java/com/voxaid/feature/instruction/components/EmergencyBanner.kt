package com.voxaid.feature.instruction.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.voxaid.core.design.theme.VoxAidTheme

/**
 * Emergency banner component for critical warnings.
 * Features pulsing animation and high visibility.
 *
 * @param message The emergency message to display
 * @param type Type of banner (Warning, Call911)
 * @param isPulsing Whether to show pulsing animation
 */
@Composable
fun EmergencyBanner(
    message: String,
    type: EmergencyBannerType = EmergencyBannerType.Warning,
    isPulsing: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "emergency_pulse")

    val alpha by infiniteTransition.animateFloat(
        initialValue = if (isPulsing) 0.7f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isPulsing) alpha else 1f)
            .semantics {
                contentDescription = "${type.name}: $message"
                liveRegion = LiveRegionMode.Assertive
            },
        colors = CardDefaults.cardColors(
            containerColor = type.backgroundColor()
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = type.icon(),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = type.contentColor()
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = type.contentColor(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Types of emergency banners.
 */
enum class EmergencyBannerType {
    Warning,
    Call911,
    Critical;

    @Composable
    fun backgroundColor() = when (this) {
        Warning -> MaterialTheme.colorScheme.errorContainer
        Call911 -> MaterialTheme.colorScheme.error
        Critical -> MaterialTheme.colorScheme.error
    }

    @Composable
    fun contentColor() = when (this) {
        Warning -> MaterialTheme.colorScheme.onErrorContainer
        Call911 -> MaterialTheme.colorScheme.onError
        Critical -> MaterialTheme.colorScheme.onError
    }

    fun icon(): ImageVector = when (this) {
        Warning -> Icons.Default.Warning
        Call911 -> Icons.Default.Phone
        Critical -> Icons.Default.Warning
    }
}

@Preview(showBackground = true)
@Composable
private fun EmergencyBannerPreview() {
    VoxAidTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EmergencyBanner(
                message = "Only perform CPR if person is unresponsive",
                type = EmergencyBannerType.Warning,
                isPulsing = true
            )

            EmergencyBanner(
                message = "Call 911 immediately before starting CPR",
                type = EmergencyBannerType.Call911,
                isPulsing = true
            )

            EmergencyBanner(
                message = "Continue compressions until help arrives",
                type = EmergencyBannerType.Critical,
                isPulsing = false
            )
        }
    }
}