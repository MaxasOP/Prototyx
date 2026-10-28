package com.example.prototyx.ui.main

import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.ui.viewmodels.DashboardViewModel
import com.example.prototyx.ui.viewmodels.RiskMeshViewModel
import com.example.prototyx.ui.viewmodels.ScreenerViewModel
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun dashboardViewModel_loadsDataOnInitialization() = runTest {
        val repository = MockDataRepository()
        val viewModel = DashboardViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.finnyReport)
        assertEquals(88, viewModel.finnyReport?.portfolioHealthScore)
    }

    @Test
    fun riskMeshViewModel_analyzesMeshSuccessfully() = runTest {
        val repository = MockDataRepository()
        val viewModel = RiskMeshViewModel(repository)
        viewModel.analyzeMesh(listOf("TCS", "RELIANCE"), listOf(0.5, 0.5))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.riskMeshResult)
        assertEquals(0.42, viewModel.riskMeshResult?.netExposureIndex)
    }

    @Test
    fun screenerViewModel_performsSearchAndFilters() = runTest {
        val repository = MockDataRepository()
        val viewModel = ScreenerViewModel(repository)
        viewModel.performSearch("Dividend quality")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.results.isNotEmpty())
        assertEquals("TCS", viewModel.results.first().ticker)
    }
}
