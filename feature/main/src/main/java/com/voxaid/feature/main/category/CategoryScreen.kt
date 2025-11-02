package com.voxaid.feature.main.category

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.voxaid.core.design.components.Call911Dialog
import com.voxaid.core.design.components.VoxAidTopBar
import com.voxaid.core.design.theme.VoxAidTheme

/**
 * Category selection screen.
 * Displays available first aid protocols (CPR, Heimlich, Bandaging).
 */
@Composable
fun CategoryScreen(
    mode: String,
    onProtocolSelected: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val isEmergency = mode == "emergency"
    val context = LocalContext.current
    var show911Dialog by remember { mutableStateOf(false) }

    val protocols = listOf(
        Protocol("cpr","CPR (Cardiopulmonary Resuscitation)","For unresponsive person not breathing", Icons.Default.Favorite),
        Protocol("heimlich","Heimlich Maneuver","For choking victim", Icons.Default.Warning),
        Protocol("bandaging","Wound Bandaging","For bleeding wounds", Icons.Default.HealthAndSafety)
    )

    // 911 dialog
    if (show911Dialog) {
        Call911Dialog(
            onConfirm = {
                show911Dialog = false
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:911")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                runCatching { context.startActivity(intent) }
            },
            onDismiss = { show911Dialog = false }
        )
    }

    Scaffold(
        topBar = {
            VoxAidTopBar(
                title = if (isEmergency) "Emergency Protocols" else "Learn Protocols",
                onBackClick = onBackClick,
                show911Button = true,
                on911Click = { show911Dialog = true }
            )
        },
        containerColor = if (isEmergency)
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f)
        else MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = if (isEmergency) "Select the emergency you're facing:"
                else "Choose a protocol to learn:",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(protocols) { protocol ->
                    CategoryProtocolCard(
                        protocol = protocol,
                        isEmergency = isEmergency,
                        onClick = { onProtocolSelected(protocol.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryProtocolCard(
    protocol: Protocol,
    isEmergency: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isEmergency) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(2.dp, borderColor),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = protocol.icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = borderColor
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = protocol.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = protocol.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Select ${protocol.name}",
                tint = borderColor
            )
        }
    }
}

private data class Protocol(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector
)

@Preview(showBackground = true)
@Composable
private fun CategoryScreenPreview() {
    VoxAidTheme {
        CategoryScreen(
            mode = "instructional",
            onProtocolSelected = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryScreenEmergencyPreview() {
    VoxAidTheme {
        CategoryScreen(
            mode = "emergency",
            onProtocolSelected = {},
            onBackClick = {}
        )
    }
}