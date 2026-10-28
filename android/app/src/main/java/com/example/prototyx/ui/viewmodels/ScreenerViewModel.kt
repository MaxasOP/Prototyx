package com.example.prototyx.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.model.PricePoint
import com.example.prototyx.data.model.ScreenerResultItem
import com.example.prototyx.data.model.StockChartResponse
import com.example.prototyx.data.model.UserExperienceMode
import com.example.prototyx.util.ProductionLogger
import kotlinx.coroutines.launch

enum class ScreenerSortOption(val displayName: String) {
    DEFAULT("Default"),
    PRICE_DESC("Price: High to Low"),
    PE_ASC("P/E: Low to High"),
    YIELD_DESC("Yield: High to Low"),
    RSI_ASC("RSI: Oversold First")
}

class ScreenerViewModel(private val repository: DataRepository) : ViewModel() {

    var queryText by mutableStateOf("")
    var selectedSector by mutableStateOf("All")
    var selectedRiskLevel by mutableStateOf("All")
    var selectedSortOption by mutableStateOf(ScreenerSortOption.DEFAULT)
    var userMode by mutableStateOf(UserExperienceMode.PRO)

    var recentSearches by mutableStateOf<List<String>>(
        listOf("Dividend quality", "High growth tech", "Undervalued large caps", "Safe tech stocks")
    )
        private set

    var results by mutableStateOf<List<ScreenerResultItem>>(emptyList())
        private set

    var selectedCompanyDetail by mutableStateOf<ScreenerResultItem?>(null)
    var companyChartData by mutableStateOf<StockChartResponse?>(null)
    var isChartLoading by mutableStateOf(false)

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        performSearch("")
    }

    fun performSearch(query: String? = null) {
        val q = query ?: queryText
        queryText = q
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val riskFilter = if (selectedRiskLevel != "All") selectedRiskLevel else null
                var fetched = repository.searchScreener(q, riskFilter)

                // Apply local sector filter if specified
                if (selectedSector != "All") {
                    fetched = fetched.filter { it.sector.contains(selectedSector, ignoreCase = true) }
                }

                // Apply sorting option
                results = when (selectedSortOption) {
                    ScreenerSortOption.PRICE_DESC -> fetched.sortedByDescending { it.price }
                    ScreenerSortOption.PE_ASC -> fetched.sortedBy { it.peRatio }
                    ScreenerSortOption.YIELD_DESC -> fetched.sortedByDescending { it.dividendYield }
                    ScreenerSortOption.RSI_ASC -> fetched.sortedBy { it.rsi }
                    ScreenerSortOption.DEFAULT -> fetched
                }

                // Update recent searches list
                if (q.isNotBlank() && !recentSearches.contains(q)) {
                    recentSearches = (listOf(q) + recentSearches).take(6)
                }
            } catch (e: Exception) {
                ProductionLogger.recordException(e)
                errorMessage = "Screener search failed: ${e.message ?: "Server connection error"}"
            } finally {
                isLoading = false
            }
        }
    }

    fun selectCompanyForDetail(item: ScreenerResultItem) {
        selectedCompanyDetail = item
        viewModelScope.launch {
            isChartLoading = true
            try {
                companyChartData = repository.getStockChart(item.ticker, "1M")
            } catch (e: Exception) {
                ProductionLogger.recordException(e)
                // Fallback chart points if network is unavailable
                companyChartData = StockChartResponse(
                    ticker = item.ticker,
                    currentPrice = item.price,
                    priceChange = item.changePercent,
                    priceChangePercent = item.changePercent,
                    pricePoints = (1..20).map { day ->
                        PricePoint("Day $day", item.price * (1f + (day * 0.002f)), rsi = item.rsi)
                    }
                )
            } finally {
                isChartLoading = false
            }
        }
    }

    fun clearCompanyDetail() {
        selectedCompanyDetail = null
        companyChartData = null
    }

    fun clearFilters() {
        selectedSector = "All"
        selectedRiskLevel = "All"
        selectedSortOption = ScreenerSortOption.DEFAULT
        performSearch(queryText)
    }
}
