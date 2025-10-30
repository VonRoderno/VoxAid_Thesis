package com.voxaid.feature.main.category

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
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
        Protocol(
            id = "cpr",
            name = "CPR (Cardiopulmonary Resuscitation)",
            description = "For unresponsive person not breathing",
            icon = Icons.Default.Favorite
        ),
        Protocol(
            id = "heimlich",
            name = "Heimlich Maneuver",
            description = "For choking victim",
            icon = Icons.Default.Warning
        ),
        Protocol(
            id = "bandaging",
            name = "Wound Bandaging",
            description = "For bleeding wounds",
            icon = Icons.Default.HealthAndSafety
        )
    )

    // Handle 911 dialog
    if (show911Dialog) {
        Call911Dialog(
            onConfirm = {
                show911Dialog = false
                // Launch dialer with 911
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:911")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    timber.log.Timber.e(e, "Failed to launch dialer")
                }
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
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = if (isEmergency) {
                        "Select the emergency you're facing:"
                    } else {
                        "Choose a protocol to learn:"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(protocols) { protocol ->
                ProtocolCard(
                    protocol = protocol,
                    isEmergency = isEmergency,
                    onClick = { onProtocolSelected(protocol.id) }
                )
            }

            if (isEmergency) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "Have you called 911?\nAlways call emergency services first!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProtocolCard(
    protocol: Protocol,
    isEmergency: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEmergency) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                tint = if (isEmergency) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = protocol.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isEmergency) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = protocol.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEmergency) {
                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    }
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Select ${protocol.name}",
                tint = if (isEmergency) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
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