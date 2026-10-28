package com.example.prototyx.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.data.model.AgentDebateLog
import com.example.prototyx.data.model.ConsultResponse
import com.example.prototyx.data.model.DebateResponse
import com.example.prototyx.theme.Amber
import com.example.prototyx.theme.Coral
import com.example.prototyx.theme.Mint
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.ui.components.*
import kotlinx.coroutines.launch
import java.util.Locale

enum class CommitteeView {
    Selection, Consultation, Debate
}

@Composable
fun CommitteeScreen(
    repository: DataRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var currentView by remember { mutableStateOf(CommitteeView.Selection) }

    var inputTickers by remember { mutableStateOf("TCS, RELIANCE, AAPL") }
    var userQuery by remember { mutableStateOf("Outlook for GenAI enterprise cloud spend in Q4?") }

    var debateResult by remember { mutableStateOf<DebateResponse?>(null) }
    var consultResult by remember { mutableStateOf<ConsultResponse?>(null) }

    var isLoadingDebate by remember { mutableStateOf(false) }
    var isLoadingConsult by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val viewsList = remember(debateResult) { debateResult?.impliedViews?.toList() ?: emptyList() }

    val sampleQueries = remember {
        listOf(
            "GenAI cloud spend in Q4",
            "Federal Reserve rate hike impact",
            "NVDA vs AAPL valuation",
            "High dividend energy stocks"
        )
    }

    val sampleTickerPresets = remember {
        listOf(
            "TCS, RELIANCE, AAPL",
            "NVDA, AAPL, MSFT",
            "RELIANCE, HDFCBANK, ITC"
        )
    }

    BackHandler(enabled = currentView != CommitteeView.Selection) {
        currentView = CommitteeView.Selection
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Header Section
            item(key = "header") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentView != CommitteeView.Selection) {
                            TerminalIconButton(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to selection",
                                onClick = { currentView = CommitteeView.Selection },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        ScreenHeader(
                            category = "PROTOTYX · RESEARCH DESK",
                            title = when (currentView) {
                                CommitteeView.Selection -> "AI Committee"
                                CommitteeView.Consultation -> "Thesis Research"
                                CommitteeView.Debate -> "Security Debate"
                            },
                            subtitle = "Adversarial multi-agent debate and thesis consultation"
                        )
                    }
                }
            }

            // 2. Selection View (Module Cards)
            if (currentView == CommitteeView.Selection) {
                item(key = "tile_consult") {
                    SelectionTileCard(
                        title = "Research an Investment Thesis",
                        subtitle = "TEST THESIS AGAINST MULTI-AGENT PERSPECTIVES",
                        description = "Evaluate macro trends, valuation risks, and technical setups with natural language AI research.",
                        badgeText = "ORCHESTRATOR CONSULT",
                        onClick = { currentView = CommitteeView.Consultation }
                    )
                }

                item(key = "tile_debate") {
                    SelectionTileCard(
                        title = "Adversarial Security Debate",
                        subtitle = "GOVERNANCE & MULTI-AGENT CONSENSUS",
                        description = "Simulate an institutional debate between Macro, Fundamental, Technical, and Risk agents on selected tickers.",
                        badgeText = "COMMITTEE DEBATE",
                        onClick = { currentView = CommitteeView.Debate }
                    )
                }
            }

            // --- SUB-VIEW 1: Thesis Consultation ---
            if (currentView == CommitteeView.Consultation) {
                item(key = "consult_input") {
                    DoubleBezelCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SectionHeader(
                                title = "Investment thesis prompt",
                                supportingText = "Ask the multi-agent research cluster to evaluate an asset or macro trend"
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            TerminalTextField(
                                value = userQuery,
                                onValueChange = { userQuery = it },
                                label = "Thesis or Research Question",
                                placeholder = "e.g., Outlook for Tech in Q4 amid rate stabilization?",
                                singleLine = false
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Sample Thesis Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                sampleQueries.forEach { prompt ->
                                    TerminalFilterChip(
                                        selected = userQuery.equals(prompt, ignoreCase = true),
                                        onClick = { userQuery = prompt },
                                        label = prompt
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            PrimaryButton(
                                text = "Run Multi-Agent Research",
                                onClick = {
                                    coroutineScope.launch {
                                        isLoadingConsult = true
                                        errorMessage = null
                                        consultResult = null
                                        debateResult = null
                                        try {
                                            consultResult = repository.runConsult(userQuery)
                                        } catch (e: Exception) {
                                            errorMessage = "Research engine error: ${e.message ?: "Service offline"}"
                                        } finally {
                                            isLoadingConsult = false
                                        }
                                    }
                                },
                                isLoading = isLoadingConsult,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    item(key = "error_consult") {
                        ErrorStateCard(
                            message = errorMessage!!,
                            onRetry = {
                                coroutineScope.launch {
                                    isLoadingConsult = true
                                    errorMessage = null
                                    try {
                                        consultResult = repository.runConsult(userQuery)
                                    } catch (e: Exception) {
                                        errorMessage = e.message
                                    } finally {
                                        isLoadingConsult = false
                                    }
                                }
                            }
                        )
                    }
                }

                if (isLoadingConsult) {
                    item(key = "loading_consult") {
                        DoubleBezelCard {
                            TerminalLoadingIndicator(statusText = "Orchestrator Executing Parallel Agent Tasks...")
                        }
                    }
                }

                // Consultation Results Display
                consultResult?.let { res ->
                    item(key = "consult_verdict") {
                        DoubleBezelCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SectionHeader(title = "Research synthesis")
                                    StatusBadge(text = res.asset ?: "HOUSE VIEW", type = StatusBadgeType.POSITIVE)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = res.recommendation ?: "No research recommendation generated.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = "Verified Source",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Source: Nemotron-3 LPU Multi-Agent Model",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    StatusBadge(text = "VERIFIED TELEMETRY", type = StatusBadgeType.NEUTRAL)
                                }
                            }
                        }
                    }
                }
            }

            // --- SUB-VIEW 2: Security Governance Debate ---
            if (currentView == CommitteeView.Debate) {
                item(key = "debate_input") {
                    DoubleBezelCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SectionHeader(
                                title = "Security selection",
                                supportingText = "Enter comma-separated ticker symbols to execute adversarial debate"
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            TerminalTextField(
                                value = inputTickers,
                                onValueChange = { inputTickers = it },
                                label = "Target Tickers",
                                placeholder = "e.g. TCS, RELIANCE, AAPL"
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Preset Ticker Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                sampleTickerPresets.forEach { preset ->
                                    TerminalFilterChip(
                                        selected = inputTickers.equals(preset, ignoreCase = true),
                                        onClick = { inputTickers = preset },
                                        label = preset
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            PrimaryButton(
                                text = "Execute Committee Debate",
                                onClick = {
                                    coroutineScope.launch {
                                        isLoadingDebate = true
                                        errorMessage = null
                                        debateResult = null
                                        consultResult = null
                                        try {
                                            val tickers = inputTickers.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
                                            debateResult = repository.runDebate(tickers)
                                        } catch (e: Exception) {
                                            errorMessage = "Debate engine failure: ${e.message ?: "Server unreachable"}"
                                        } finally {
                                            isLoadingDebate = false
                                        }
                                    }
                                },
                                isLoading = isLoadingDebate,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    item(key = "error_debate") {
                        ErrorStateCard(
                            message = errorMessage!!,
                            onRetry = {
                                coroutineScope.launch {
                                    isLoadingDebate = true
                                    errorMessage = null
                                    try {
                                        val tickers = inputTickers.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
                                        debateResult = repository.runDebate(tickers)
                                    } catch (e: Exception) {
                                        errorMessage = e.message
                                    } finally {
                                        isLoadingDebate = false
                                    }
                                }
                            }
                        )
                    }
                }

                if (isLoadingDebate) {
                    item(key = "loading_debate") {
                        DoubleBezelCard {
                            TerminalLoadingIndicator(statusText = "Multi-Agent Committee In Session...")
                        }
                    }
                }

                // Display Debate Consensus & Agent Perspective Cards
                debateResult?.let { res ->
                    item(key = "executive_consensus_card") {
                        ExecutiveConsensusCard(
                            actionVerdict = "ACCUMULATE CORE ALLOCATION",
                            badgeType = StatusBadgeType.POSITIVE,
                            confidence = 88,
                            takeaway = "Macro & Fundamental agents confirm solid cash flow expansion across selections. Technical indicators signal bullish consolidation, while Risk & Compliance enforces a 25% max single-asset position limit."
                        )
                    }

                    // Highlights: Consensus vs Disagreement Summary
                    item(key = "consensus_disagreement_summary") {
                        DoubleBezelCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SectionHeader(
                                    title = "Consensus & disagreement breakdown",
                                    supportingText = "Points of alignment and key risk constraints"
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = "Agreement",
                                            tint = Mint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Core Growth Alignment",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Fundamental & Technical agents unanimously support expanding exposure based on cloud transformation contracts and favorable 50-DMA technical support.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Warning,
                                            contentDescription = "Disagreement",
                                            tint = Amber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Risk & Compliance Enforcement",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Risk Agent flags high sector correlation between IT holdings, capping maximum single-asset weight at 25% to protect against systematic drawdown.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "debate_logs_header") {
                        SectionHeader(
                            title = "Analyst perspectives & notes",
                            supportingText = "Detailed reasoning by agent role"
                        )
                    }

                    itemsIndexed(res.debateLogs ?: emptyList(), key = { idx, log -> "agent_card_$idx" }) { _, log ->
                        AnalystPerspectiveCard(log = log)
                    }

                    item(key = "implied_views_header") {
                        SectionHeader(
                            title = "Projected annual return assumptions",
                            supportingText = "Implied return views generated by Black-Litterman multi-agent model"
                        )
                    }

                    items(viewsList, key = { it.first }) { (ticker, view) ->
                        val formattedView = remember(view) { String.format(Locale.US, "%.1f%%", view * 100) }
                        FinancialDataRow(
                            title = ticker,
                            subtitle = "Projected Return Expectation",
                            value = formattedView,
                            isPositive = view >= 0
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

/**
 * SelectionTileCard: Tile card for selecting research thesis or committee debate sub-modules.
 */
@Composable
fun SelectionTileCard(
    title: String,
    subtitle: String,
    description: String,
    badgeText: String,
    onClick: () -> Unit
) {
    DoubleBezelCard(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetadataLabel(text = subtitle, color = MaterialTheme.colorScheme.primary)
                StatusBadge(text = badgeText, type = StatusBadgeType.PRIMARY)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "ACCESS MODULE",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * AnalystPerspectiveCard: Expandable card for individual agent/analyst perspectives.
 */
@Composable
fun AnalystPerspectiveCard(
    log: AgentDebateLog
) {
    var expanded by remember { mutableStateOf(false) }
    val (agentColor, agentRole) = remember(log.agent) {
        when {
            log.agent.contains("Fundamental", ignoreCase = true) -> Mint to "FUNDAMENTAL ANALYST"
            log.agent.contains("Technical", ignoreCase = true) -> Amber to "TECHNICAL ANALYST"
            log.agent.contains("Compliance", ignoreCase = true) -> Coral to "RISK & COMPLIANCE AGENT"
            else -> Color(0xFF9ABEB3) to "MACRO ANALYST"
        }
    }

    DoubleBezelCard(
        modifier = Modifier.clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(agentColor)
                    )
                    Text(
                        text = log.agent,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                StatusBadge(text = agentRole, type = StatusBadgeType.NEUTRAL)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = log.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Source",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Source: yFinance & SEC Filings",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Text(
                    text = if (expanded) "Collapse reasoning ▲" else "Expand full reasoning ▼",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * ExecutiveConsensusCard: House consensus and cross-discipline confidence card.
 */
@Composable
fun ExecutiveConsensusCard(
    actionVerdict: String,
    badgeType: StatusBadgeType,
    confidence: Int,
    takeaway: String
) {
    DoubleBezelCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(title = "House View")
                StatusBadge(text = actionVerdict, type = badgeType)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$confidence% Agreement",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    MetadataLabel(text = "CROSS-DISCIPLINE COMMITTEE CONFIDENCE")
                }

                AnimatedScoreRing(score = confidence, color = Mint)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = takeaway,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CommitteeScreenPreview() {
    PrototyxTheme {
        CommitteeScreen(repository = MockDataRepository())
    }
}
