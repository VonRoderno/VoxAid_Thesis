package com.voxaid.feature.main.variant

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voxaid.core.design.components.Call911Dialog
import com.voxaid.core.design.components.VoxAidTopBar
import com.voxaid.core.design.theme.VoxAidTheme
import com.voxaid.feature.main.components.LockedVariantDialog

/**
 * Protocol variant selection screen with lock/unlock UI.
 * Shows lock icons for variants not yet completed in instructional mode.
 */
@Composable
fun ProtocolVariantScreen(
    protocolId: String,
    mode: String,
    onVariantSelected: (String) -> Unit,
    onBackClick: () -> Unit,
    onNavigateToInstructional: (String) -> Unit,
    viewModel: ProtocolVariantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var show911Dialog by remember { mutableStateOf(false) }
    var showLockedDialog by remember { mutableStateOf<ProtocolLockState?>(null) }

    // Handle 911 dialog
    if (show911Dialog) {
        Call911Dialog(
            onConfirm = {
                show911Dialog = false
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

    // Handle locked variant dialog
    showLockedDialog?.let { lockState ->
        LockedVariantDialog(
            variantName = lockState.name,
            onDismiss = { showLockedDialog = null },
            onGoToInstructional = {
                showLockedDialog = null
                // Navigate back to main menu to select instructional mode
//                onBackClick()
                onNavigateToInstructional(lockState.variantId)
            }
        )
    }

    Scaffold(
        topBar = {
            VoxAidTopBar(
                title = getProtocolTitle(protocolId),
                onBackClick = onBackClick,
                show911Button = true,
                on911Click = { show911Dialog = true }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is VariantScreenUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is VariantScreenUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Show info banner for emergency mode
                        if (state.isEmergencyMode) {
                            EmergencyModeBanner(
                                unlockedCount = state.unlockedCount,
                                totalCount = state.totalCount,
                                allLocked = state.allLocked
                            )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text(
                                    text = if (state.isEmergencyMode) {
                                        "Select the situation:"
                                    } else {
                                        "Choose a variant to learn:"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            items(state.variants) { variant ->
                                VariantCardWithLock(
                                    lockState = variant,
                                    isEmergencyMode = state.isEmergencyMode,
                                    onClick = {
                                        if (variant.canSelect(state.isEmergencyMode)) {
                                            onVariantSelected(variant.variantId)
                                        } else {
                                            // Show locked dialog
                                            showLockedDialog = variant
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                is VariantScreenUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error Loading Variants",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyModeBanner(
    unlockedCount: Int,
    totalCount: Int,
    allLocked: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (allLocked) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (allLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = if (allLocked) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (allLocked) {
                            "All Variants Locked"
                        } else {
                            "Unlocked: $unlockedCount/$totalCount"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (allLocked) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )

                    Text(
                        text = if (allLocked) {
                            "Complete these protocols in Instructional Mode to unlock them for emergencies"
                        } else {
                            "Complete more in Instructional Mode to unlock additional variants"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (allLocked) {
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun VariantCardWithLock(
    lockState: ProtocolLockState,
    isEmergencyMode: Boolean,
    onClick: () -> Unit
) {
    val isLocked = isEmergencyMode && !lockState.isUnlocked

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !isEmergencyMode -> MaterialTheme.colorScheme.primaryContainer
                isLocked -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.errorContainer
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isLocked) 0.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lock/Unlock icon
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else getVariantIcon(lockState.variantId),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = when {
                        !isEmergencyMode -> MaterialTheme.colorScheme.onPrimaryContainer
                        isLocked -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onErrorContainer
                    }
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = lockState.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = when {
                                !isEmergencyMode -> MaterialTheme.colorScheme.onPrimaryContainer
                                isLocked -> MaterialTheme.colorScheme.onSurfaceVariant
                                else -> MaterialTheme.colorScheme.onErrorContainer
                            }
                        )

                        if (isEmergencyMode && lockState.isUnlocked) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Unlocked",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = lockState.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            !isEmergencyMode -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            isLocked -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            else -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        }
                    )

                    // Show lock message for locked variants
                    if (isLocked) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = lockState.lockMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        // Show progress if user has started
                        if (lockState.showProgress) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Column {
                                Text(
                                    text = "Progress: ${lockState.completion?.progressPercentage}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                LinearProgressIndicator(
                                    progress = { (lockState.completion?.progressPercentage ?: 0) / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.ArrowForward,
                    contentDescription = if (isLocked) "Locked" else "Select ${lockState.name}",
                    tint = when {
                        !isEmergencyMode -> MaterialTheme.colorScheme.onPrimaryContainer
                        isLocked -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }
        }
    }
}

/**
 * Get icon for variant based on ID.
 */
private fun getVariantIcon(variantId: String): ImageVector {
    return when {
        variantId.contains("1person") -> Icons.Default.Person
        variantId.contains("2person") -> Icons.Default.People
        variantId.contains("aed") -> Icons.Default.Favorite
        variantId.contains("no_aed") -> Icons.Default.FavoriteBorder
        variantId.contains("others") -> Icons.Default.HealthAndSafety
        variantId.contains("self") -> Icons.Default.AccessibilityNew
        variantId.contains("triangular") -> Icons.Default.ChangeHistory
        variantId.contains("circular") -> Icons.Default.FiberManualRecord
        else -> Icons.Default.HealthAndSafety
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
private fun EmergencyModeBannerPreview() {
    VoxAidTheme {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EmergencyModeBanner(
                unlockedCount = 0,
                totalCount = 4,
                allLocked = true
            )

            EmergencyModeBanner(
                unlockedCount = 2,
                totalCount = 4,
                allLocked = false
            )
        }
    }
}