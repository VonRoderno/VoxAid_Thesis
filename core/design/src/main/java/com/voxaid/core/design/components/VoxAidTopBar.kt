package com.voxaid.core.design.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.voxaid.core.design.theme.VoxAidTheme

/**
 * Custom top app bar for VoxAid.
 * Includes optional back button, microphone status indicator, and Call 911 button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoxAidTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    showMicIndicator: Boolean = false,
    isMicActive: Boolean = false,
    show911Button: Boolean = true,
    on911Click: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge
            )
        },
        modifier = modifier,
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.semantics {
                        contentDescription = "Navigate back"
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        },
        actions = {
            // Mic indicator
            if (showMicIndicator) {
                MicChip(
                    isActive = isMicActive,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            // Call 911 button
            if (show911Button && on911Click != null) {
                IconButton(
                    onClick = on911Click,
                    modifier = Modifier.semantics {
                        contentDescription = "Call 911 emergency services"
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call 911",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun VoxAidTopBarPreview() {
    VoxAidTheme {
        Column {
            VoxAidTopBar(
                title = "CPR Instructions",
                on911Click = {}
            )

            Spacer(modifier = Modifier.height(8.dp))

            VoxAidTopBar(
                title = "Emergency Mode",
                onBackClick = {},
                showMicIndicator = true,
                isMicActive = true,
                show911Button = true,
                on911Click = {}
            )

            Spacer(modifier = Modifier.height(8.dp))

            VoxAidTopBar(
                title = "Main Menu",
                show911Button = false
            )
        }
    }
}