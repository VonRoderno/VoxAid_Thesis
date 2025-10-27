package com.voxaid.core.design.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.voxaid.core.design.theme.VoxAidTheme

/**
 * Custom top app bar for VoxAid.
 * Includes optional back button and microphone status indicator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoxAidTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    showMicIndicator: Boolean = false,
    isMicActive: Boolean = false,
    show911Button: Boolean = true
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
            if (showMicIndicator) {
                MicChip(
                    isActive = isMicActive,
                    modifier = Modifier.padding(end = 8.dp)
                )
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
                title = "CPR Instructions"
            )

            Spacer(modifier = Modifier.height(8.dp))

            VoxAidTopBar(
                title = "Emergency Mode",
                onBackClick = {},
                showMicIndicator = true,
                isMicActive = true
            )
        }
    }
}