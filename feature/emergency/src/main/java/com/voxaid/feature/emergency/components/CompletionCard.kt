//package com.voxaid.feature.emergency.components
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.CheckCircle
//import androidx.compose.material.icons.filled.Info
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import com.voxaid.core.content.model.EmergencyStep
//import com.voxaid.core.design.theme.VoxAidTheme
//
///**
// * Completion card shown at end of emergency protocol.
// * Shows success/info message and elapsed time.
// */
//@Composable
//fun CompletionCard(
//    step: EmergencyStep.CompletionStep,
//    totalElapsedSeconds: Int,
//    onFinish: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    val minutes = totalElapsedSeconds / 60
//    val seconds = totalElapsedSeconds % 60
//
//    Column(
//        modifier = modifier
//            .fillMaxSize()
//            .padding(32.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        // Icon
//        Icon(
//            imageVector = if (step.isSuccess) {
//                Icons.Default.CheckCircle
//            } else {
//                Icons.Default.Info
//            },
//            contentDescription = null,
//            modifier = Modifier.size(96.dp),
//            tint = if (step.isSuccess) {
//                MaterialTheme.colorScheme.primary
//            } else {
//                MaterialTheme.colorScheme.error
//            }
//        )
//
//        Spacer(modifier = Modifier.height(32.dp))
//        // Title
//        Text(
//            text = step.title,
//            style = MaterialTheme.typography.displayMedium,
//            fontWeight = FontWeight.Bold,
//            color = MaterialTheme.colorScheme.onErrorContainer,
//            textAlign = TextAlign.Center
//        )
//
//        Spacer(modifier = Modifier.height(24.dp))
//
//        // Message
//        Text(
//            text = step.message,
//            style = MaterialTheme.typography.headlineSmall,
//            color = MaterialTheme.colorScheme.onErrorContainer,
//            textAlign = TextAlign.Center,
//            lineHeight = MaterialTheme.typography.headlineSmall.lineHeight.times(1.3f)
//        )
//
//        Spacer(modifier = Modifier.height(32.dp))
//
//        // Elapsed time
//        Card(
//            colors = CardDefaults.cardColors(
//                containerColor = MaterialTheme.colorScheme.primaryContainer
//            ),
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(20.dp),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Text(
//                    text = "Total Time",
//                    style = MaterialTheme.typography.titleMedium,
//                    color = MaterialTheme.colorScheme.onPrimaryContainer
//                )
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                Text(
//                    text = String.format("%02d:%02d", minutes, seconds),
//                    style = MaterialTheme.typography.displayLarge,
//                    fontWeight = FontWeight.Bold,
//                    color = MaterialTheme.colorScheme.primary
//                )
//
//                Text(
//                    text = "minutes:seconds",
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
//                )
//            }
//        }
//
//        Spacer(modifier = Modifier.height(48.dp))
//
//        // Finish button
//        Button(
//            onClick = onFinish,
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(64.dp),
//            colors = ButtonDefaults.buttonColors(
//                containerColor = if (step.isSuccess) {
//                    MaterialTheme.colorScheme.primary
//                } else {
//                    MaterialTheme.colorScheme.error
//                }
//            )
//        ) {
//            Text(
//                text = "Finish",
//                style = MaterialTheme.typography.titleLarge,
//                fontWeight = FontWeight.Bold
//            )
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Additional info
//        Text(
//            text = if (step.isSuccess) {
//                "Emergency services should arrive shortly"
//            } else {
//                "Tap Finish to return to category screen"
//            },
//            style = MaterialTheme.typography.bodyMedium,
//            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
//            textAlign = TextAlign.Center
//        )
//    }
//}
//@Preview(showBackground = true)
//@Composable
//private fun CompletionCardSuccessPreview() {
//    VoxAidTheme {
//        Surface(color = MaterialTheme.colorScheme.errorContainer) {
//            CompletionCard(
//                step = EmergencyStep.CompletionStep(
//                    stepId = "success",
//                    title = "Patient Responsive!",
//                    message = "The patient is showing signs of life.\n\n" +
//                            "Continue monitoring until emergency services arrive.\n\n" +
//                            "You did an amazing job!",
//                    isSuccess = true
//                ),
//                totalElapsedSeconds = 185,
//                onFinish = {}
//            )
//        }
//    }
//}
//@Preview(showBackground = true)
//@Composable
//private fun CompletionCardExhaustedPreview() {
//    VoxAidTheme {
//        Surface(color = MaterialTheme.colorScheme.errorContainer) {
//            CompletionCard(
//                step = EmergencyStep.CompletionStep(
//                    stepId = "exhausted",
//                    title = "Rest if Needed",
//                    message = "If you're exhausted, it's okay to rest.\n\n" +
//                            "Wait for emergency services to arrive.\n\n" +
//                            "You did great by trying your best to help.",
//                    isSuccess = true
//                ),
//                totalElapsedSeconds = 240,
//                onFinish = {}
//            )
//        }
//    }
//}
