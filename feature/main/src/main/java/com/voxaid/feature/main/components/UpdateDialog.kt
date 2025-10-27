package com.voxaid.feature.main.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.voxaid.core.common.model.UpdateInfo
import com.voxaid.core.design.theme.VoxAidTheme

/**
 * Update dialog shown when a new version is available.
 * Can be mandatory or optional based on UpdateInfo.
 */
@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onUpdate: () -> Unit,
    onDismiss: (() -> Unit)?
) {
    AlertDialog(
        onDismissRequest = {
            if (!updateInfo.isMandatory) {
                onDismiss?.invoke()
            }
        },
        title = {
            Text(
                text = if (updateInfo.isMandatory) {
                    "Update Required"
                } else {
                    "Update Available"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Version ${updateInfo.latestVersion} is now available.",
                    style = MaterialTheme.typography.bodyLarge
                )

                if (updateInfo.isMandatory) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "This update is required to continue using VoxAid.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "What's New:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = updateInfo.releaseNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight.times(1.5f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onUpdate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (updateInfo.isMandatory) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                Text("Update Now")
            }
        },
        dismissButton = if (onDismiss != null && !updateInfo.isMandatory) {
            {
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
            }
        } else null,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}

@Preview
@Composable
private fun UpdateDialogPreview() {
    VoxAidTheme {
        UpdateDialog(
            updateInfo = UpdateInfo(
                latestVersion = "1.1.0",
                minimumVersion = "1.0.0",
                updateUrl = "https://play.google.com/store/apps/details?id=com.voxaid.app",
                releaseNotes = "• Improved voice recognition\n• Bug fixes\n• Performance improvements",
                isMandatory = false
            ),
            onUpdate = {},
            onDismiss = {}
        )
    }
}

@Preview
@Composable
private fun MandatoryUpdateDialogPreview() {
    VoxAidTheme {
        UpdateDialog(
            updateInfo = UpdateInfo(
                latestVersion = "2.0.0",
                minimumVersion = "2.0.0",
                updateUrl = "https://play.google.com/store/apps/details?id=com.voxaid.app",
                releaseNotes = "• Critical security updates\n• New protocol added\n• Performance improvements",
                isMandatory = true
            ),
            onUpdate = {},
            onDismiss = null
        )
    }
}