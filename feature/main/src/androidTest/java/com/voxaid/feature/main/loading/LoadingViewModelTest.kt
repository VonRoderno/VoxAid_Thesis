package com.voxaid.feature.main.loading
//TODO - figure out how to test
//import com.voxaid.core.common.datastore.PreferencesManager
//import com.voxaid.core.common.model.UpdateCheckResult
//import com.voxaid.core.common.model.UpdateInfo
//import com.voxaid.core.common.network.UpdateCheckService
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
// * Unit tests for LoadingViewModel.
// * Tests disclaimer and update check logic.
// */
//@OptIn(ExperimentalCoroutinesApi::class)
//class LoadingViewModelTest {
//
//    private lateinit var viewModel: LoadingViewModel
//    private lateinit var updateCheckService: UpdateCheckService
//    private lateinit var preferencesManager: PreferencesManager
//
//    private val testDispatcher = StandardTestDispatcher()
//
//    @Before
//    fun setup() {
//        Dispatchers.setMain(testDispatcher)
//
//        updateCheckService = mock()
//        preferencesManager = mock()
//    }
//
//    @After
//    fun tearDown() {
//        Dispatchers.resetMain()
//    }
//
//    @Test
//    fun `init with disclaimer not accepted shows disclaimer`() = runTest {
//        // Given
//        whenever(preferencesManager.disclaimerAccepted).thenReturn(flowOf(false))
//
//        // When
//        viewModel = LoadingViewModel(updateCheckService, preferencesManager)
//        advanceUntilIdle()
//
//        // Then
//        val state = viewModel.uiState.first()
//        assertTrue(state is LoadingUiState.UpdateAvailable)
//        assertEquals(updateInfo, (state as LoadingUiState.UpdateAvailable).updateInfo)
//    }
//
//    @Test
//    fun `acceptDisclaimer updates preferences and continues`() = runTest {
//        // Given
//        whenever(preferencesManager.disclaimerAccepted).thenReturn(flowOf(false))
//        whenever(updateCheckService.checkForUpdate()).thenReturn(
//            UpdateCheckResult.NoUpdateNeeded
//        )
//
//        viewModel = LoadingViewModel(updateCheckService, preferencesManager)
//        advanceUntilIdle()
//
//        // When
//        viewModel.acceptDisclaimer()
//        advanceUntilIdle()
//
//        // Then
//        verify(preferencesManager).setDisclaimerAccepted(true)
//        verify(updateCheckService).checkForUpdate()
//    }
//
//    @Test
//    fun `dismissUpdate proceeds to ready state`() = runTest {
//        // Given
//        val updateInfo = UpdateInfo(
//            latestVersion = "1.1.0",
//            minimumVersion = "1.0.0",
//            updateUrl = "https://example.com",
//            releaseNotes = "Bug fixes",
//            isMandatory = false
//        )
//        whenever(preferencesManager.disclaimerAccepted).thenReturn(flowOf(true))
//        whenever(updateCheckService.checkForUpdate()).thenReturn(
//            UpdateCheckResult.UpdateAvailable(updateInfo)
//        )
//
//        viewModel = LoadingViewModel(updateCheckService, preferencesManager)
//        advanceUntilIdle()
//
//        // When
//        viewModel.dismissUpdate()
//        advanceUntilIdle()
//
//        // Then
//        val state = viewModel.uiState.first()
//        assertTrue(state is LoadingUiState.ReadyToProceed)
//    }
//}
//assertTrue(state is LoadingUiState.ShowDisclaimer)
//}
//
//@Test
//fun `init with disclaimer accepted checks for update`() = runTest {
//    // Given
//    whenever(preferencesManager.disclaimerAccepted).thenReturn(flowOf(true))
//    whenever(updateCheckService.checkForUpdate()).thenReturn(
//        UpdateCheckResult.NoUpdateNeeded
//    )
//
//    // When
//    viewModel = LoadingViewModel(updateCheckService, preferencesManager)
//    advanceUntilIdle()
//
//    // Then
//    verify(updateCheckService).checkForUpdate()
//    val state = viewModel.uiState.first()
//    assertTrue(state is LoadingUiState.ReadyToProceed)
//}
//
//@Test
//fun `update available shows update dialog`() = runTest {
//    // Given
//    val updateInfo = UpdateInfo(
//        latestVersion = "1.1.0",
//        minimumVersion = "1.0.0",
//        updateUrl = "https://example.com",
//        releaseNotes = "Bug fixes",
//        isMandatory = false
//    )
//    whenever(preferencesManager.disclaimerAccepted).thenReturn(flowOf(true))
//    whenever(updateCheckService.checkForUpdate()).thenReturn(
//        UpdateCheckResult.UpdateAvailable(updateInfo)
//    )
//
//    // When
//    viewModel = LoadingViewModel(updateCheckService, preferencesManager)
//    advanceUntilIdle()
//
//    // Then
//    val state = viewModel.uiState.first()