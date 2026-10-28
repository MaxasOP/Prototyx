package com.example.prototyx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.data.model.ScreenerResultItem
import com.example.prototyx.data.model.UserExperienceMode
import com.example.prototyx.theme.Amber
import com.example.prototyx.theme.Coral
import com.example.prototyx.theme.Mint
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.ui.components.*
import com.example.prototyx.ui.viewmodels.ScreenerSortOption
import com.example.prototyx.ui.viewmodels.ScreenerViewModel
import kotlinx.coroutines.launch

@Composable
fun ScreenerScreen(
    repository: DataRepository,
    modifier: Modifier = Modifier,
    viewModel: ScreenerViewModel = remember { ScreenerViewModel(repository) }
) {
    val coroutineScope = rememberCoroutineScope()
    var showFilterSheet by remember { mutableStateOf(false) }

    val presetQueries = remember {
        listOf(
            "Dividend quality",
            "High growth tech",
            "Undervalued large caps",
            "Low RSI momentum"
        )
    }

    val sectorOptions = remember {
        listOf("All", "Technology", "Energy", "Consumer Tech", "Banking")
    }

    val riskOptions = remember {
        listOf("All", "SAFE", "BALANCED", "GROWTH")
    }

    val hasActiveFilters = remember(viewModel.selectedSector, viewModel.selectedRiskLevel, viewModel.selectedSortOption) {
        viewModel.selectedSector != "All" || viewModel.selectedRiskLevel != "All" || viewModel.selectedSortOption != ScreenerSortOption.DEFAULT
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Terminal Header Section
            item(key = "header") {
                ScreenHeader(
                    category = "PROTOTYX · EQUITY SCREENER",
                    title = "Screener & Research",
                    subtitle = "Scan stocks using natural language queries, valuation ratios, and risk metrics"
                )
            }

            // 2. Mode Toggle (Guided vs Terminal / Simple vs Pro)
            item(key = "mode_toggle") {
                ModeToggleBar(
                    currentMode = viewModel.userMode,
                    onModeChanged = { viewModel.userMode = it }
                )
            }

            // 3. Search Bar Input Field
            item(key = "search_bar") {
                TerminalSearchBar(
                    query = viewModel.queryText,
                    onQueryChange = { newQuery ->
                        viewModel.queryText = newQuery
                        viewModel.performSearch(newQuery)
                    },
                    placeholder = "Search stocks e.g. 'Safe tech stocks with P/E < 30'...",
                    onSearch = { query -> viewModel.performSearch(query) },
                    onClear = { viewModel.performSearch("") }
                )
            }

            // 4. Preset Queries & Search History
            item(key = "preset_queries") {
                Column {
                    MetadataLabel(text = "PRESET STRATEGIES & POPULAR SEARCHES")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetQueries.forEach { preset ->
                            TerminalFilterChip(
                                selected = viewModel.queryText.equals(preset, ignoreCase = true),
                                onClick = { viewModel.performSearch(preset) },
                                label = preset
                            )
                        }
                    }
                }
            }

            // 5. Compact Filters & Sorting Controls Bar
            item(key = "filters_bar") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { showFilterSheet = true },
                            shape = RoundedCornerShape(12.dp),
                            color = if (hasActiveFilters) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (hasActiveFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.List,
                                    contentDescription = "Filters",
                                    tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (hasActiveFilters) "Filters (Active)" else "Filters",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasActiveFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (hasActiveFilters) {
                            TerminalTextButton(
                                text = "Clear All",
                                onClick = { viewModel.clearFilters() },
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Text(
                        text = "${viewModel.results.size} Results",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Error State
            if (viewModel.errorMessage != null) {
                item(key = "error_state") {
                    ErrorStateCard(
                        message = viewModel.errorMessage!!,
                        onRetry = { viewModel.performSearch() }
                    )
                }
            }

            // Loading Indicator
            if (viewModel.isLoading) {
                item(key = "loading_state") {
                    DoubleBezelCard {
                        TerminalLoadingIndicator(statusText = "Scanning Quantitative Database...")
                    }
                }
            } else if (viewModel.results.isEmpty()) {
                item(key = "empty_state") {
                    EmptyState(
                        title = "No Matching Equities Found",
                        description = "Try relaxing your search query or reset active filters.",
                        action = "Reset Filters & Search",
                        onAction = {
                            viewModel.queryText = ""
                            viewModel.clearFilters()
                        }
                    )
                }
            } else {
                // 6. Search Result Cards List
                items(viewModel.results, key = { it.ticker }) { item ->
                    ScreenerResultCard(
                        item = item,
                        userMode = viewModel.userMode,
                        onClick = { viewModel.selectCompanyForDetail(item) }
                    )
                }
            }
        }
    }

    // 7. Filter Options Bottom Sheet / Dialog
    if (showFilterSheet) {
        AlertDialog(
            onDismissRequest = { showFilterSheet = false },
            title = {
                Text(
                    text = "Refine Screener Filters",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sector Filter Section
                    Column {
                        MetadataLabel(text = "SECTOR")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            sectorOptions.forEach { sector ->
                                TerminalFilterChip(
                                    selected = (viewModel.selectedSector == sector),
                                    onClick = {
                                        viewModel.selectedSector = sector
                                        viewModel.performSearch()
                                    },
                                    label = sector
                                )
                            }
                        }
                    }

                    // Risk Level Filter Section
                    Column {
                        MetadataLabel(text = "RISK CLASSIFICATION")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            riskOptions.forEach { risk ->
                                TerminalFilterChip(
                                    selected = (viewModel.selectedRiskLevel == risk),
                                    onClick = {
                                        viewModel.selectedRiskLevel = risk
                                        viewModel.performSearch()
                                    },
                                    label = risk
                                )
                            }
                        }
                    }

                    // Sort Order Section
                    Column {
                        MetadataLabel(text = "SORT BY METRIC")
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ScreenerSortOption.entries.forEach { sortOpt ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clickable {
                                            viewModel.selectedSortOption = sortOpt
                                            viewModel.performSearch()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = sortOpt.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (viewModel.selectedSortOption == sortOpt) FontWeight.Bold else FontWeight.Normal,
                                        color = if (viewModel.selectedSortOption == sortOpt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (viewModel.selectedSortOption == sortOpt) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                PrimaryButton(
                    text = "Apply Filters",
                    onClick = { showFilterSheet = false }
                )
            },
            dismissButton = {
                TerminalTextButton(
                    text = "Reset All",
                    onClick = {
                        viewModel.clearFilters()
                        showFilterSheet = false
                    },
                    color = MaterialTheme.colorScheme.error
                )
            }
        )
    }

    // 8. Company Research Detail Dialog / Drawer
    viewModel.selectedCompanyDetail?.let { item ->
        CompanyResearchDetailDialog(
            item = item,
            chartData = viewModel.companyChartData,
            isChartLoading = viewModel.isChartLoading,
            userMode = viewModel.userMode,
            onAddToPortfolio = {
                coroutineScope.launch {
                    repository.updateHolding(item.ticker, 0.20f)
                }
                viewModel.clearCompanyDetail()
            },
            onDismiss = { viewModel.clearCompanyDetail() }
        )
    }
}

@Composable
fun ScreenerResultCard(
    item: ScreenerResultItem,
    userMode: UserExperienceMode,
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${item.ticker} • ${item.sector}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                RiskBadge(riskLevel = item.riskLevel)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quantitative Metrics Row (Pro / Terminal Mode)
            if (userMode == UserExperienceMode.PRO) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        MetadataLabel(text = "PRICE")
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "₹${item.price}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column {
                        MetadataLabel(text = "P/E RATIO")
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${item.peRatio}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column {
                        MetadataLabel(text = "DIV YIELD")
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${item.dividendYield}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Mint
                        )
                    }

                    Column {
                        MetadataLabel(text = "RSI SCORE")
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${item.rsi}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (item.rsi < 35f) Mint else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Price: ₹${item.price}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "1D Change: +${item.changePercent}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = Mint,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Summary / AI Analysis Memo
            Text(
                text = if (userMode == UserExperienceMode.SIMPLE) item.plainEnglishSummary else item.aiAnalysis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * CompanyResearchDetailDialog: Comprehensive company research overlay view.
 */
@Composable
fun CompanyResearchDetailDialog(
    item: ScreenerResultItem,
    chartData: com.example.prototyx.data.model.StockChartResponse?,
    isChartLoading: Boolean,
    userMode: UserExperienceMode,
    onAddToPortfolio: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${item.ticker} • ${item.sector}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RiskBadge(riskLevel = item.riskLevel)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Price & Metric Overview Grid
                DoubleBezelCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            MetadataLabel(text = "CURRENT PRICE")
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "₹${item.price}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column {
                            MetadataLabel(text = "P/E RATIO")
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${item.peRatio}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column {
                            MetadataLabel(text = "DIV YIELD")
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${item.dividendYield}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Mint
                            )
                        }
                    }
                }

                // Interactive Stock Chart Section
                if (isChartLoading) {
                    TerminalLoadingIndicator(statusText = "Loading Technical Chart Points...")
                } else if (chartData != null) {
                    StockChartView(
                        ticker = chartData.ticker,
                        pricePoints = chartData.pricePoints,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // AI Intelligence Memo
                DoubleBezelCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "AI Memo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (userMode == UserExperienceMode.SIMPLE) "Guided Investment Summary" else "AI Quantitative Analysis Memo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (userMode == UserExperienceMode.SIMPLE) item.plainEnglishSummary else item.aiAnalysis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Add to Allocation (+20%)",
                onClick = onAddToPortfolio
            )
        },
        dismissButton = {
            TerminalTextButton(
                text = "Close",
                onClick = onDismiss
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ScreenerScreenPreview() {
    PrototyxTheme {
        ScreenerScreen(repository = MockDataRepository())
    }
}
