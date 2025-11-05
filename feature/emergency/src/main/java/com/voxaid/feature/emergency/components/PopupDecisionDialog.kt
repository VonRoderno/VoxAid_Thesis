//package com.voxaid.feature.emergency.components
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Mic
//import androidx.compose.material.icons.filled.Warning
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.window.Dialog
//import androidx.compose.ui.window.DialogProperties
//import com.voxaid.core.design.theme.VoxAidTheme
//import com.voxaid.feature.emergency.PopupSource
//import com.voxaid.feature.emergency.PopupState
//
///**
// * Popup decision dialog for Emergency Mode.
// * Shows critical decision points with large, accessible buttons.
// * Supports voice command selection.
// *
// * Design:
// * - Full-screen priority (cannot dismiss)
// * - Large buttons for emergency context
// * - Voice command indicators
// * - High contrast colors
// */
//@Composable
//fun PopupDecisionDialog(
//    popup: PopupState,
//    onOptionSelected: (String) -> Unit,
//    onDismiss: () -> Unit
//) {
//    Dialog(
//        onDismissRequest = { /* Not dismissible */ },
//        properties = DialogProperties(
//            dismissOnBackPress = false,
//            dismissOnClickOutside = false,
//            usePlatformDefaultWidth = false
//        )
//    ) {
//        Surface(
//            modifier = Modifier
//                .fillMaxWidth(0.95f)
//                .wrapContentHeight(),
//            shape = MaterialTheme.shapes.large,
//            color = MaterialTheme.colorScheme.surface,
//            tonalElevation = 8.dp
//        ) {
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(24.dp),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                // Icon
//                Icon(
//                    imageVector = Icons.Default.Warning,
//                    contentDescription = null,
//                    modifier = Modifier.size(56.dp),
//                    tint = MaterialTheme.colorScheme.error
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Title
//                Text(
//                    text = popup.title,
//                    style = MaterialTheme.typography.headlineMedium,
//                    fontWeight = FontWeight.Bold,
//                    color = MaterialTheme.colorScheme.onSurface,
//                    textAlign = TextAlign.Center
//                )
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Message
//                Text(
//                    text = popup.message,
//                    style = MaterialTheme.typography.bodyLarge,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    textAlign = TextAlign.Center,
//                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight.times(1.4f)
//                )
//
//                Spacer(modifier = Modifier.height(24.dp))
//
//                // Voice command hint
//                if (popup.voiceEnabled) {
//                    Card(
//                        colors = CardDefaults.cardColors(
//                            containerColor = MaterialTheme.colorScheme.primaryContainer
//                        ),
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Row(
//                            modifier = Modifier.padding(12.dp),
//                            verticalAlignment = Alignment.CenterVertically,
//                            horizontalArrangement = Arrangement.Center
//                        ) {
//                            Icon(
//                                imageVector = Icons.Default.Mic,
//                                contentDescription = null,
//                                modifier = Modifier.size(20.dp),
//                                tint = MaterialTheme.colorScheme.onPrimaryContainer
//                            )
//                            Spacer(modifier = Modifier.width(8.dp))
//                            Text(
//                                text = "Say your choice or tap below",
//                                style = MaterialTheme.typography.bodyMedium,
//                                color = MaterialTheme.colorScheme.onPrimaryContainer
//                            )
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.height(16.dp))
//                }
//
//                // Options as large buttons
//                popup.options.forEach { option ->
//                    Button(
//                        onClick = { onOptionSelected(option) },
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(64.dp),
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = if (option.lowercase().contains("no") ||
//                                option.lowercase().contains("stop") ||
//                                option.lowercase().contains("exhausted")) {
//                                MaterialTheme.colorScheme.error
//                            } else {
//                                MaterialTheme.colorScheme.primary
//                            }
//                        )
//                    ) {
//                        Text(
//                            text = option,
//                            style = MaterialTheme.typography.titleLarge,
//                            fontWeight = FontWeight.Bold
//                        )
//                    }
//
//                    Spacer(modifier = Modifier.height(12.dp))
//                }
//            }
//        }
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//private fun PopupDecisionDialogPreview() {
//    VoxAidTheme {
//        PopupDecisionDialog(
//            popup = PopupState(
//                title = "Check Patient",
//                message = "Check pulse, breathing, and tap + shout 'Are you okay?'\n\n" +
//                        "Say NO if all 3 are negative\n" +
//                        "Say YES if at least one is positive",
//                options = listOf("No", "Yes"),
//                optionActions = mapOf("No" to "next", "Yes" to "end"),
//                voiceEnabled = true,
//                source = PopupSource.StepBased
//            ),
//            onOptionSelected = {},
//            onDismiss = {}
//        )
//    }
//}