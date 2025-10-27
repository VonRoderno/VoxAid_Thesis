package com.voxaid.feature.instruction.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.voxaid.core.content.model.Protocol
import com.voxaid.core.content.model.Step
import com.voxaid.core.design.theme.VoxAidTheme

/**
 * Horizontal pager for instruction steps.
 * Supports swipe gestures and programmatic navigation.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InstructionStepPager(
    protocol: Protocol,
    currentStepIndex: Int,
    isEmergencyMode: Boolean,
    onPageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = currentStepIndex,
        pageCount = { protocol.steps.size }
    )

    // Sync pager state with external step index
    LaunchedEffect(currentStepIndex) {
        if (pagerState.currentPage != currentStepIndex) {
            pagerState.animateScrollToPage(currentStepIndex)
        }
    }

    // Notify when page changes via swipe
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != currentStepIndex) {
            onPageChanged(pagerState.currentPage)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier,
        userScrollEnabled = !isEmergencyMode // Disable swipe in emergency mode
    ) { page ->
        StepCard(
            step = protocol.steps[page],
            isEmergencyMode = isEmergencyMode,
            totalSteps = protocol.steps.size
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InstructionStepPagerPreview() {
    VoxAidTheme {
        val sampleProtocol = Protocol(
            id = "sample",
            name = "Sample Protocol",
            description = "Sample",
            category = "test",
            estimatedDurationMinutes = 5,
            steps = listOf(
                Step(
                    stepNumber = 1,
                    title = "Step 1",
                    description = "First step description",
                    voicePrompt = "Do the first step"
                ),
                Step(
                    stepNumber = 2,
                    title = "Step 2",
                    description = "Second step description",
                    voicePrompt = "Do the second step"
                )
            )
        )

        InstructionStepPager(
            protocol = sampleProtocol,
            currentStepIndex = 0,
            isEmergencyMode = false,
            onPageChanged = {}
        )
    }
}