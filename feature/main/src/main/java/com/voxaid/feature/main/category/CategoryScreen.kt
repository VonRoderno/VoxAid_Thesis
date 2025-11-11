package com.voxaid.feature.main.category

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voxaid.core.common.datastore.EmergencyLockState
import com.voxaid.core.design.components.Call911Dialog
import com.voxaid.core.design.components.VoxAidTopBar
import com.voxaid.core.design.theme.VoxAidTheme
import timber.log.Timber

/**
 * Category selection screen with emergency lock badges.
 * Shows unlock progress for emergency mode protocols.
 *
 * Updated: Displays lock state and progress at category level
 */
@Composable
fun CategoryScreen(
    mode: String,
    onProtocolSelected: (String) -> Unit,
    onBackClick: () -> Unit,
    onGoToInstructional: (String) -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var show911Dialog by remember { mutableStateOf(false) }
    var showLockedDialog by remember { mutableStateOf<CategoryState?>(null) }

    // Handle 911 dialog
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

    // Handle locked protocol dialog
    showLockedDialog?.let { categoryState ->
        LockedProtocolDialog(
            protocolName = categoryState.protocol.name,
            lockState = categoryState.lockState,
            onDismiss = { showLockedDialog = null },
            onGoToInstructional = {
                showLockedDialog = null
                onBackClick() // Return to mode selection
//                onGoToInstructional(categoryState.protocol.id)
            }
        )
    }

    Scaffold(
        topBar = {
            VoxAidTopBar(
                title = if (mode == "emergency") "🚨 Emergency Protocols" else "Learn Protocols",
                onBackClick = onBackClick,
                show911Button = true,
                on911Click = { show911Dialog = true }
            )
        },
        containerColor = if (mode == "emergency")
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f)
        else MaterialTheme.colorScheme.background
    ) { paddingValues ->

        when (val state = uiState) {
            is CategoryUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is CategoryUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Emergency mode banner
                    if (state.isEmergencyMode) {
                        EmergencyModeBanner(
                            allLocked = state.allLocked,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    Text(
                        text = if (state.isEmergencyMode)
                            "Select the emergency you're facing:"
                        else
                            "Choose a protocol to learn:",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(state.categories) { categoryState ->
                            CategoryCard(
                                categoryState = categoryState,
                                isEmergencyMode = state.isEmergencyMode,
                                onClick = {
                                    if (viewModel.canSelectProtocol(categoryState.protocol.id)) {
                                        onProtocolSelected(categoryState.protocol.id)
                                        Timber.d("Category clicked -> name: ${categoryState.protocol.name}, , protocol: ${categoryState.protocol.id}")
                                    } else {
                                        showLockedDialog = categoryState
                                    }
                                }
                            )
                        }
                    }
                }
            }

            is CategoryUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error Loading Categories",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = state.message)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyModeBanner(
    allLocked: Boolean,
    modifier: Modifier = Modifier
) {
    if (allLocked) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "🔒 All Protocols Locked",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Complete training in Instructional Mode to unlock Emergency Mode",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    categoryState: CategoryState,
    isEmergencyMode: Boolean,
    onClick: () -> Unit
) {
    val protocol = categoryState.protocol
    val lockState = categoryState.lockState
    val isLocked = isEmergencyMode && !lockState.isUnlocked

    val borderColor = when {
        !isEmergencyMode -> MaterialTheme.colorScheme.primary
        isLocked -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isLocked) 0.dp else 2.dp
        ),
        border = BorderStroke(2.dp, borderColor),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = when {
                    isLocked -> Icons.Default.Lock
                    protocol.id == "cpr" -> Icons.Default.Favorite
                    protocol.id == "heimlich" -> Icons.Default.Warning
                    else -> Icons.Default.HealthAndSafety
                },
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = borderColor
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = protocol.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isLocked) FontWeight.Normal else FontWeight.SemiBold
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
                    text = protocol.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                // Lock info
                if (isEmergencyMode) {
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLocked) {
                        // Show progress
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = lockState.lockMessage,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        if (lockState.progress.second > 0) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Column {
                                Text(
                                    text = "${lockState.progress.first}/${lockState.progress.second} training completed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                LinearProgressIndicator(
                                    progress = { lockState.progressPercentage / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                )
                            }
                        }
                    } else {
                        // Show unlocked badge
                        Text(
                            text = "✓ ${lockState.lockMessage}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Icon(
                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.ArrowForward,
                contentDescription = if (isLocked) "Locked" else "Select ${protocol.name}",
                tint = borderColor
            )
        }
    }
}

@Composable
fun LockedProtocolDialog(
    protocolName: String,
    lockState: EmergencyLockState,
    onDismiss: () -> Unit,
    onGoToInstructional: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lock_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lock_scale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(scale)
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        title = {
            Text(
                text = "🔒 $protocolName Locked",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$protocolName is currently locked in Emergency Mode.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = lockState.detailedMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Required Training:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        lockState.requiredVariants.forEach { variant ->
                            val isCompleted = variant in lockState.completedVariants
                            TrainingItem(
                                title = variant.replace("_", " ")
                                    .replaceFirstChar { it.uppercase() },
                                isCompleted = isCompleted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 Complete all required training to unlock emergency access.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onGoToInstructional,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "Go to Training",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 12.dp
    )
}

@Composable
private fun TrainingItem(title: String, isCompleted: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isCompleted)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryScreenPreview() {
    VoxAidTheme {
        // Preview would use mock data
    }
}
