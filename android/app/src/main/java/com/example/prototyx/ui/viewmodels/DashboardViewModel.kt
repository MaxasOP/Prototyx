package com.example.prototyx.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.model.*
import com.example.prototyx.util.ProductionLogger
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(private val repository: DataRepository) : ViewModel() {

    val holdings: StateFlow<Map<String, Float>> = repository.holdings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    var selectedTicker by mutableStateOf("")
        private set

    var tickerData by mutableStateOf<MarketIndicatorsResponse?>(null)
        private set

    var chartData by mutableStateOf<StockChartResponse?>(null)
        private set

    var newsFeed by mutableStateOf<List<NewsSentimentItem>>(emptyList())
        private set

    var finnyReport by mutableStateOf<FinnyStrategyReport?>(null)
        private set

    var userMode by mutableStateOf(UserExperienceMode.PRO)

    var isLoading by mutableStateOf(false)
        private set

    var isSyncing by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            try {
                finnyReport = repository.getFinnyReport()
                newsFeed = repository.getNewsSentiment()
            } catch (e: Exception) {
                ProductionLogger.recordException(e)
            }
        }
    }

    fun selectTicker(ticker: String) {
        if (ticker.isBlank()) return
        selectedTicker = ticker
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                tickerData = repository.getIndicators(ticker)
                chartData = repository.getStockChart(ticker)
            } catch (e: Exception) {
                ProductionLogger.recordException(e)
                errorMessage = "Failed to extract intelligence for $ticker"
            } finally {
                isLoading = false
            }
        }
    }

    fun updateHolding(ticker: String, weight: Float) {
        viewModelScope.launch {
            try {
                if (weight > 0) {
                    repository.updateHolding(ticker, weight)
                } else {
                    repository.removeHolding(ticker)
                }
                selectTicker(ticker)
            } catch (e: Exception) {
                ProductionLogger.recordException(e)
            }
        }
    }

    fun syncCloud() {
        viewModelScope.launch {
            isSyncing = true
            try {
                repository.syncWithCloud()
            } catch (e: Exception) {
                ProductionLogger.recordException(e)
            } finally {
                isSyncing = false
            }
        }
    }
}
