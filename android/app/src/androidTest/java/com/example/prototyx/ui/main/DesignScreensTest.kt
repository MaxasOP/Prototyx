package com.example.prototyx.ui.main

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.ui.screens.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.prototyx.ui.components.NewsSentimentCard
import com.example.prototyx.data.model.NewsSentimentItem
import org.junit.Rule
import org.junit.Test
import java.io.File

class DesignScreensTest {
    @get:Rule val rule = createComposeRule()
    @Test fun portfolioRenderedReview() {
        capture("portfolio") { DashboardScreen(MockDataRepository(), {}) }
    }
    @Test fun equitiesRenderedReview() { capture("equities") { ScreenerScreen(MockDataRepository()) } }
    @Test fun allocationRenderedReview() { capture("allocation") { OptimizerScreen(MockDataRepository()) } }
    @Test fun riskRenderedReview() { capture("risk") { RiskMeshScreen(MockDataRepository()) } }
    @Test fun researchRenderedReview() { capture("research") { CommitteeScreen(MockDataRepository()) } }
    @Test fun earningsRenderedReview() { capture("earnings") { EarningsScreen(MockDataRepository()) } }
    @Test fun loginRenderedReview() { capture("login") { LoginScreen(MockDataRepository(), {}) } }
    @Test fun newsRenderedReview() {
        capture("news") {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                NewsSentimentCard(NewsSentimentItem("review", "Company reports quarterly results and updates its capital expenditure plans", "Example publisher", "2026-10-29T10:30:00+00:00", listOf("TCS"), "UNASSESSED", 0f, "Not assessed", "Not assessed", "https://example.com/article"))
            }
        }
    }
    private fun capture(name: String, content: @Composable () -> Unit) {
        rule.setContent { PrototyxTheme { androidx.compose.material3.Surface { content() } } }
        if (name == "equities") {
            rule.waitUntil(timeoutMillis = 5000) { rule.onAllNodesWithText("Tata Consultancy Services").fetchSemanticsNodes().isNotEmpty() }
        }
        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val directory = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)!!
        File(directory, "$name-design.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
