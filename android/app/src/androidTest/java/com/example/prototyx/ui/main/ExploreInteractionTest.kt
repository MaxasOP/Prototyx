package com.example.prototyx.ui.main

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.data.model.TickerSearchResponse
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.ui.screens.*
import kotlinx.coroutines.delay
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ExploreInteractionTest {
    @get:Rule val rule = createComposeRule()
    private val companies = listOf(
        TickerSearchResponse("TCS", "Tata Consultancy Services", "", "", "NSE"),
        TickerSearchResponse("TSLA", "Tesla", "", "", "NASDAQ"))

    @Test fun typingFiltersImmediatelyAndSearchesBeyondTheStartingList() {
        val repo = object: DataRepository by MockDataRepository() {
            override suspend fun searchTickers(query: String?, sector: String?): List<TickerSearchResponse> {
                if (query.isNullOrBlank()) return companies
                delay(600)
                return if (query.contains("shop", true)) listOf(TickerSearchResponse("SHOP", "Shopify", "", "", "NASDAQ"))
                    else companies.filter { it.name.contains(query, true) || it.ticker.contains(query, true) }
            }
        }
        rule.setContent { PrototyxTheme { ScreenerScreen(repo) } }
        rule.waitUntil(5000) { rule.onAllNodesWithText("Tesla").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Company or ticker").performTextInput("tes")
        rule.onNodeWithText("Tata Consultancy Services").assertDoesNotExist()
        rule.onNodeWithText("Tesla").assertExists()
        rule.onNodeWithText("Company or ticker").performTextReplacement("shopify")
        rule.onNodeWithText("Tesla").assertDoesNotExist()
        rule.waitUntil(5000) { rule.onAllNodesWithText("Shopify").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("SHOP · NASDAQ").assertExists()
    }

    @Test fun typingDoesNotFetchQuotesAndSnapshotIsExplicit() {
        var quoteCalls = 0
        val repo = object: DataRepository by MockDataRepository() {
            override suspend fun searchTickers(query: String?, sector: String?) = companies.take(1)
            override suspend fun getIndicators(ticker: String): com.example.prototyx.data.model.MarketIndicatorsResponse {
                quoteCalls++; return MockDataRepository().getIndicators(ticker)
            }
        }
        rule.setContent { PrototyxTheme { ScreenerScreen(repo) } }
        rule.waitUntil(5000) { rule.onAllNodesWithText("Tata Consultancy Services").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(0, quoteCalls)
        rule.onNodeWithText("View market snapshot").performClick()
        rule.waitForIdle()
        assertEquals(1, quoteCalls)
        rule.onNodeWithText("1000.00 ").assertExists()
    }

    @Test fun researchActionsDescribeTheirDestination() {
        rule.setContent { PrototyxTheme { CommitteeScreen(MockDataRepository()) } }
        rule.onNodeWithText("Find earnings →").assertExists()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Ask a research question →"))
        rule.onNodeWithText("Ask a research question →").assertExists()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Compare companies →"))
        rule.onNodeWithText("Compare companies →").assertExists()
        rule.onNodeWithText("Open research →").assertDoesNotExist()
    }
}
