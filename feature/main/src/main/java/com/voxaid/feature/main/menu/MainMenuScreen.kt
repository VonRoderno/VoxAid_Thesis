package com.voxaid.feature.main.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.voxaid.core.design.theme.VoxAidTheme

@Composable
fun MainMenuScreen(
    onModeSelected: (String) -> Unit
) {
    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .imePadding(), // handle keyboard overlap safely
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Scrollable content area for smaller screens
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .padding(vertical = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info Text
                Text(
                    text = "⚠️ Remember: Always call emergency services first!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
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

    val bgColor = MaterialTheme.colorScheme.surface
    val borderColor = if (isEmergency) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(gradient),
                shape = RoundedCornerShape(20.dp)
            )
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clickable { onClick() }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Start")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MainMenuScreenPreview() {
    VoxAidTheme {
        MainMenuScreen(onModeSelected = {})
    }
}
