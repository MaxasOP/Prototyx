package com.example.prototyx.ui.main

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.ui.components.AllocationDialog
import com.example.prototyx.ui.screens.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class JourneyTest {
    @get:Rule val rule = createComposeRule()
    private class Portfolio(initial: Map<String, Float> = emptyMap()) : DataRepository by MockDataRepository() {
        override val holdings = MutableStateFlow(initial)
        var replaced = false
        override suspend fun updateHolding(ticker: String, weight: Float) { holdings.value = holdings.value + (ticker to weight) }
        override suspend fun removeHolding(ticker: String) { holdings.value = holdings.value - ticker }
        override suspend fun replaceHoldings(holdings: Map<String, Float>) { replaced = true; this.holdings.value = holdings }
    }

    @Test fun discoveryCarriesCompanyThroughResearchAndEarningsIntoAllocation() {
        val repository = Portfolio()
        rule.setContent { PrototyxTheme {
            var screen by remember { mutableStateOf("explore") }
            var ticker by remember { mutableStateOf("") }
            var allocation by remember { mutableStateOf(false) }
            when(screen) {
                "explore" -> ScreenerScreen(repository, onResearch = { ticker = it; screen = "research" })
                "research" -> CommitteeScreen(repository, initialTicker = ticker, onEarnings = { ticker = it; screen = "earnings" })
                "earnings" -> EarningsScreen(repository, initialTicker = ticker, onAddHolding = { ticker = it; allocation = true })
            }
            if (allocation) AllocationDialog(ticker, repository, { allocation = false }, { allocation = false; screen = "portfolio" })
            if (screen == "portfolio") DashboardScreen(repository, {})
        } }
        rule.waitUntil(5000) { rule.onAllNodesWithText("Tata Consultancy Services").fetchSemanticsNodes().isNotEmpty() }
        rule.onAllNodesWithText("Research").onFirst().performClick()
        rule.onNodeWithText("What are the investment case, valuation risks and key uncertainties for TCS?").assertExists()
        rule.onNodeWithText("Read earnings").performClick()
        rule.onNodeWithText("TCS", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Open transcript").performScrollTo().performClick()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Set allocation"))
        rule.onNodeWithText("Set allocation").performClick()
        rule.onNodeWithText("Portfolio weight (%)").performTextInput("20")
        rule.onNodeWithText("Save allocation").performClick()
        rule.waitForIdle()
        assertEquals(.2f, repository.holdings.value.getValue("TCS"), .0001f)
        rule.onNodeWithText("20.0%").assertExists()
    }

    @Test fun overallocatedInputCannotSaveAndZeroRemovesHolding() {
        val repository = Portfolio(mapOf("TCS" to .8f, "AAPL" to .2f))
        rule.setContent { PrototyxTheme { AllocationDialog("AAPL", repository, {}, {}) } }
        rule.onNodeWithText("Portfolio weight (%)").performTextReplacement("30")
        rule.onNodeWithText("Save allocation").performClick()
        rule.onNodeWithText("Reduce another holding first. Only 20.0% is available.").assertExists()
        assertEquals(.2f, repository.holdings.value.getValue("AAPL"), .0001f)
        rule.onNodeWithText("Portfolio weight (%)").performTextReplacement("0")
        rule.onNodeWithText("Save allocation").performClick()
        rule.waitForIdle()
        assertFalse(repository.holdings.value.containsKey("AAPL"))
    }

    @Test fun replacingPortfolioRequiresConfirmationAndCanBeCancelled() {
        val repository = Portfolio(mapOf("TCS" to .8f, "AAPL" to .2f))
        rule.setContent { PrototyxTheme { OptimizerScreen(repository) } }
        rule.onNodeWithText("Calculate target").performScrollTo().performClick()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Apply target allocation"))
        rule.onNodeWithText("Apply target allocation").performClick()
        assertFalse(repository.replaced)
        rule.onNodeWithText("Keep portfolio").performClick()
        assertFalse(repository.replaced)
        rule.onNodeWithText("Apply target allocation").performClick()
        rule.onNodeWithText("Replace allocation").performClick()
        rule.waitForIdle()
        assertTrue(repository.replaced)
        rule.onNodeWithText("Target allocation saved.").assertExists()
    }

    @Test fun emptyPortfolioStartsWithDiscovery() {
        var opened = false
        rule.setContent { PrototyxTheme { DashboardScreen(Portfolio(), {}, onExplore = { opened = true }) } }
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Find a company"))
        rule.onNodeWithText("Find a company").performClick()
        assertTrue(opened)
        rule.onNodeWithText("Model allocations").assertDoesNotExist()
        rule.onNodeWithText("Weight (%)").assertDoesNotExist()
    }
}
