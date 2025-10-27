package com.voxaid.feature.main.variant

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.voxaid.core.design.components.VoxAidTopBar
import com.voxaid.core.design.theme.VoxAidTheme

/**
 * Protocol variant selection screen.
 * Shows specific variants for each protocol type.
 */
@Composable
fun ProtocolVariantScreen(
    protocolId: String,
    mode: String,
    onVariantSelected: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val isEmergency = mode == "emergency"
    val variants = getProtocolVariants(protocolId)

    Scaffold(
        topBar = {
            VoxAidTopBar(
                title = getProtocolTitle(protocolId),
                onBackClick = onBackClick
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
                        "Select the situation:"
                    } else {
                        "Choose a variant to learn:"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(variants) { variant ->
                VariantCard(
                    variant = variant,
                    isEmergency = isEmergency,
                    onClick = { onVariantSelected(variant.id) }
                )
            }
        }
    }
}

@Composable
private fun VariantCard(
    variant: ProtocolVariant,
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
                imageVector = variant.icon,
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
                    text = variant.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isEmergency) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = variant.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEmergency) {
                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    }
                )

                if (variant.difficulty != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Difficulty: ${variant.difficulty}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isEmergency) {
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        }
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Select ${variant.name}",
                tint = if (isEmergency) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
        }
    }
}

/**
 * Data class for protocol variant.
 */
data class ProtocolVariant(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val difficulty: String? = null
)

/**
 * Get variants for a specific protocol.
 */
private fun getProtocolVariants(protocolId: String): List<ProtocolVariant> {
    return when (protocolId) {
        "cpr" -> listOf(
            ProtocolVariant(
                id = "cpr_1person",
                name = "1-Person CPR",
                description = "Standard CPR performed by one rescuer",
                icon = Icons.Default.Person,
                difficulty = "Beginner"
            ),
            ProtocolVariant(
                id = "cpr_2person",
                name = "2-Person CPR",
                description = "CPR with two rescuers alternating compressions",
                icon = Icons.Default.People,
                difficulty = "Intermediate"
            ),
            ProtocolVariant(
                id = "cpr_aed",
                name = "CPR with AED",
                description = "CPR combined with Automated External Defibrillator",
                icon = Icons.Default.Favorite,
                difficulty = "Advanced"
            ),
            ProtocolVariant(
                id = "cpr_no_aed",
                name = "CPR without AED",
                description = "Standard CPR when AED is not available",
                icon = Icons.Default.FavoriteBorder,
                difficulty = "Beginner"
            )
        )

        "heimlich" -> listOf(
            ProtocolVariant(
                id = "heimlich_others",
                name = "Heimlich for Others",
                description = "Perform Heimlich maneuver on another person",
                icon = Icons.Default.HealthAndSafety,
                difficulty = "Beginner"
            ),
            ProtocolVariant(
                id = "heimlich_self",
                name = "Self Heimlich",
                description = "Perform Heimlich maneuver on yourself",
                icon = Icons.Default.AccessibilityNew,
                difficulty = "Intermediate"
            )
        )

        "bandaging" -> listOf(
            ProtocolVariant(
                id = "bandaging_triangular",
                name = "Triangular Bandaging",
                description = "Using triangular bandages for slings and wounds",
                icon = Icons.Default.ChangeHistory,
                difficulty = "Beginner"
            ),
            ProtocolVariant(
                id = "bandaging_circular",
                name = "Circular Bandaging",
                description = "Roller bandage technique for limbs and joints",
                icon = Icons.Default.FiberManualRecord,
                difficulty = "Intermediate"
            )
        )

        else -> emptyList()
    }
}

/**
 * Get protocol display title.
 */
private fun getProtocolTitle(protocolId: String): String {
    return when (protocolId) {
        "cpr" -> "CPR Variants"
        "heimlich" -> "Heimlich Maneuver Variants"
        "bandaging" -> "Bandaging Techniques"
        else -> "Select Variant"
    }
}

@Preview(showBackground = true)
@Composable
private fun ProtocolVariantScreenPreview() {
    VoxAidTheme {
        ProtocolVariantScreen(
            protocolId = "cpr",
            mode = "instructional",
            onVariantSelected = {},
            onBackClick = {}
        )
    }
}