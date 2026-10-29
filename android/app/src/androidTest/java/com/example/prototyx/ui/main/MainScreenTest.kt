package com.example.prototyx.ui.main

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.ui.screens.*
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test fun earningsRejectsBlankTicker() {
        rule.setContent { PrototyxTheme { EarningsScreen(MockDataRepository()) } }
        rule.onNodeWithText("Open transcript").performClick()
        rule.onNodeWithText("Enter a ticker, fiscal year and quarter from 1 to 4.").assertExists()
    }

    @Test fun optimizerShowsResultsForSelectedAssets() {
        rule.setContent { PrototyxTheme { OptimizerScreen(MockDataRepository()) } }
        rule.onNodeWithText("Calculate target").performScrollTo().performClick()
        rule.onNodeWithText("Apply target allocation").performScrollTo().assertExists()
    }

    @Test fun researchDoesNotShowFabricatedConsensus() {
        rule.setContent { PrototyxTheme { CommitteeScreen(MockDataRepository()) } }
        rule.onNodeWithText("ACCUMULATE CORE ALLOCATION").assertDoesNotExist()
        rule.onNodeWithText("88% agreement").assertDoesNotExist()
    }
}
