package com.voxaid.feature.main.loading

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voxaid.core.design.theme.VoxAidTheme
import com.voxaid.feature.main.components.DisclaimerDialog
import com.voxaid.feature.main.components.UpdateDialog

/**
 * Loading screen with VoxAid branding.
 * Shows disclaimer on first run and checks for updates.
 */
@Composable
fun LoadingScreen(
    onNavigateToMenu: () -> Unit,
    viewModel: LoadingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate when ready
    LaunchedEffect(uiState) {
        if (uiState is LoadingUiState.ReadyToProceed) {
            onNavigateToMenu()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Branding
            Text(
                text = "VoxAid",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Voice-Guided Emergency Aid",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            when (uiState) {
                is LoadingUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is LoadingUiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (uiState as LoadingUiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { onNavigateToMenu() }) {
                            Text("Proceed Anyway")
                        }
                    }
                }
                else -> {}
            }
        }
    }

    // Show disclaimer dialog on first run
    if (uiState is LoadingUiState.ShowDisclaimer) {
        DisclaimerDialog(
            onAccept = { viewModel.acceptDisclaimer() }
        )
    }

    // Show update dialog if available
    if (uiState is LoadingUiState.UpdateAvailable) {
        val updateInfo = (uiState as LoadingUiState.UpdateAvailable).updateInfo
        UpdateDialog(
            updateInfo = updateInfo,
            onUpdate = {
                // TODO: Navigate to Play Store
                viewModel.dismissUpdate()
            },
            onDismiss = if (updateInfo.isMandatory) null else {
                { viewModel.dismissUpdate() }
            }
        )
    }
}


@Preview(showBackground = true)
@Composable
private fun LoadingScreenPreview() {
    VoxAidTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = "VoxAid",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Voice-Guided Emergency Aid",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}