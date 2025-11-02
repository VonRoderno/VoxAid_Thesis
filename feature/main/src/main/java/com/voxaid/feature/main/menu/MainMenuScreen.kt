package com.voxaid.feature.main.menu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.voxaid.core.design.components.EmergencyButton
import com.voxaid.core.design.components.PrimaryButton
import com.voxaid.core.design.theme.VoxAidTheme

/**
 * Main menu screen.
 * User selects between Instructional or Emergency mode.
 */
@Composable
fun MainMenuScreen(
    onModeSelected: (String) -> Unit
) {
    Scaffold(
        topBar = {
            // Empty top bar for main menu
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Text(
                text = "VoxAid",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Choose Your Mode",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Mode Selection Cards
            ModeSelectionCard(
                title = "Instructional Mode",
                description = "Learn first aid procedures at your own pace with voice guidance and step-by-step instructions.",
                icon = Icons.Default.School,
                onClick = { onModeSelected("instructional") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            ModeSelectionCard(
                title = "Emergency Mode",
                description = "Real-time voice-guided emergency assistance with large text and auto-advancing steps.",
                icon = Icons.Default.LocalHospital,
                isEmergency = true,
                onClick = { onModeSelected("emergency") }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Info text
            Text(
                text = "Remember: Always call emergency services first!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun ModeSelectionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isEmergency: Boolean = false
) {
    // Choose gradient depending on mode
    val gradient = if (isEmergency) {
        listOf(
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        )
    } else {
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
    }

    // Container background stays surface for readability
    val bgColor = MaterialTheme.colorScheme.surface

    val borderColor = if (isEmergency) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .background(bgColor)
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(gradient),
                shape = RoundedCornerShape(20.dp)
            )
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = borderColor
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = borderColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Start")
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun MainMenuScreenPreview() {
    VoxAidTheme {
        MainMenuScreen(
            onModeSelected = {}
        )
    }
}