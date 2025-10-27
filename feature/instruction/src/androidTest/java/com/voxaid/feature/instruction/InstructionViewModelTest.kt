package com.voxaid.feature.instruction
//TODO - figure out testing
//import androidx.lifecycle.SavedStateHandle
//import com.voxaid.app.navigation.VoxAidRoute
//import com.voxaid.core.common.datastore.PreferencesManager
//import com.voxaid.core.common.tts.TtsManager
//import com.voxaid.core.content.model.Protocol
//import com.voxaid.core.content.model.Step
//import com.voxaid.core.content.repository.ProtocolRepository
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.ExperimentalCoroutinesApi
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.flow.flowOf
//import kotlinx.coroutines.test.*
//import org.junit.After
//import org.junit.Before
//import org.junit.Test
//import org.mockito.kotlin.*
//import kotlin.test.assertEquals
//import kotlin.test.assertTrue
//
///**
// * Unit tests for InstructionViewModel.
// */
//@OptIn(ExperimentalCoroutinesApi::class)
//class InstructionViewModelTest {
//
//    private lateinit var viewModel: InstructionViewModel
//    private lateinit var protocolRepository: ProtocolRepository
//    private lateinit var ttsManager: TtsManager
//    private lateinit var preferencesManager: PreferencesManager
//    private lateinit var savedStateHandle: SavedStateHandle
//
//    private val testDispatcher = StandardTestDispatcher()
//
//    private val sampleProtocol = Protocol(
//        id = "test",
//        name = "Test Protocol",
//        description = "Test",
//        category = "test",
//        estimatedDurationMinutes = 5,
//        steps = listOf(
//            Step(
//                stepNumber = 1,
//                title = "Step 1",
//                description = "First step",
//                voicePrompt = "Do step 1"
//            ),
//            Step(
//                stepNumber = 2,
//                title = "Step 2",
//                description = "Second step",
//                voicePrompt = "Do step 2"
//            ),
//            Step(
//                stepNumber = 3,
//                title = "Step 3",
//                description = "Third step",
//                voicePrompt = "Do step 3"
//            )
//        )
//    )
//
//    @Before
//    fun setup() {
//        Dispatchers.setMain(testDispatcher)
//
//        protocolRepository = mock()
//        ttsManager = mock()
//        preferencesManager = mock()
//
//        savedStateHandle = SavedStateHandle().apply {
//            set(VoxAidRoute.Instruction.ARG_MODE, "instructional")
//            set(VoxAidRoute.Instruction.ARG_PROTOCOL, "test")
//        }
//
//        whenever(preferencesManager.ttsEnabled).thenReturn(flowOf(true))
//        whenever(preferencesManager.autoAdvanceEnabled).thenReturn(flowOf(false))
//        whenever(ttsManager.ttsEvents).thenReturn(flowOf())
//    }
//
//    @After
//    fun tearDown() {
//        Dispatchers.resetMain()
//    }
//
//    @Test
//    fun `previousStep goes back to previous step`() = runTest {
//        // Given
//        whenever(protocolRepository.getProtocol("test"))
//            .thenReturn(Result.success(sampleProtocol))
//
//        viewModel = InstructionViewModel(
//            savedStateHandle,
//            protocolRepository,
//            ttsManager,
//            preferencesManager
//        )
//        advanceUntilIdle()
//
//        // Move to step 2
//        viewModel.nextStep()
//        advanceUntilIdle()
//
//        // When
//        viewModel.previousStep()
//        advanceUntilIdle()
//
//        // Then
//        assertEquals(0, viewModel.currentStepIndex.first())
//        val state = viewModel.uiState.first() as InstructionUiState.Success
//        assertEquals(sampleProtocol.steps[0], state.currentStep)
//    }
//
//    @Test
//    fun `goToStep jumps to specific step`() = runTest {
//        // Given
//        whenever(protocolRepository.getProtocol("test"))
//            .thenReturn(Result.success(sampleProtocol))
//
//        viewModel = InstructionViewModel(
//            savedStateHandle,
//            protocolRepository,
//            ttsManager,
//            preferencesManager
//        )
//        advanceUntilIdle()
//
//        // When
//        viewModel.goToStep(2)
//        advanceUntilIdle()
//
//        // Then
//        assertEquals(2, viewModel.currentStepIndex.first())
//        val state = viewModel.uiState.first() as InstructionUiState.Success
//        assertEquals(sampleProtocol.steps[2], state.currentStep)
//    }
//
//    @Test
//    fun `repeatStep speaks current step again`() = runTest {
//        // Given
//        whenever(protocolRepository.getProtocol("test"))
//            .thenReturn(Result.success(sampleProtocol))
//
//        viewModel = InstructionViewModel(
//            savedStateHandle,
//            protocolRepository,
//            ttsManager,
//            preferencesManager
//        )
//        advanceUntilIdle()
//
//        // Clear previous invocations
//        clearInvocations(ttsManager)
//
//        // When
//        viewModel.repeatStep()
//        advanceUntilIdle()
//
//        // Then
//        verify(ttsManager).speak(sampleProtocol.steps[0].voicePrompt)
//    }
//
//    @Test
//    fun `protocol load failure shows error state`() = runTest {
//        // Given
//        whenever(protocolRepository.getProtocol("test"))
//            .thenReturn(Result.failure(Exception("Load failed")))
//
//        // When
//        viewModel = InstructionViewModel(
//            savedStateHandle,
//            protocolRepository,
//            ttsManager,
//            preferencesManager
//        )
//        advanceUntilIdle()
//
//        // Then
//        val state = viewModel.uiState.first()
//        assertTrue(state is InstructionUiState.Error)
//    }
//} `init loads protocol successfully`() = runTest {
//    // Given
//    whenever(protocolRepository.getProtocol("test"))
//        .thenReturn(Result.success(sampleProtocol))
//
//    // When
//    viewModel = InstructionViewModel(
//        savedStateHandle,
//        protocolRepository,
//        ttsManager,
//        preferencesManager
//    )
//    advanceUntilIdle()
//
//    // Then
//    val state = viewModel.uiState.first()
//    assertTrue(state is InstructionUiState.Success)
//    assertEquals(sampleProtocol, (state as InstructionUiState.Success).protocol)
//    assertEquals(sampleProtocol.steps[0], state.currentStep)
//}
//
//@Test
//fun `nextStep advances to next step`() = runTest {
//    // Given
//    whenever(protocolRepository.getProtocol("test"))
//        .thenReturn(Result.success(sampleProtocol))
//
//    viewModel = InstructionViewModel(
//        savedStateHandle,
//        protocolRepository,
//        ttsManager,
//        preferencesManager
//    )
//    advanceUntilIdle()
//
//    // When
//    viewModel.nextStep()
//    advanceUntilIdle()
//
//    // Then
//    assertEquals(1, viewModel.currentStepIndex.first())
//    val state = viewModel.uiState.first() as InstructionUiState.Success
//    assertEquals(sampleProtocol.steps[1], state.currentStep)
//}
//
//@Test
//fun