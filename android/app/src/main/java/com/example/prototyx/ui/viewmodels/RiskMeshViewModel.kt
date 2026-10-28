package com.example.prototyx.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.model.RiskMeshResponse
import com.example.prototyx.data.model.StressTestResult
import com.example.prototyx.util.ProductionLogger
import kotlinx.coroutines.launch

class RiskMeshViewModel(private val repository: DataRepository) : ViewModel() {

    var riskMeshResult by mutableStateOf<RiskMeshResponse?>(null)
        private set

    var stressTestResult by mutableStateOf<StressTestResult?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isStressLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun analyzeMesh(tickers: List<String>, weights: List<Double>) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                riskMeshResult = repository.getRiskMesh(tickers, weights)
            } catch (e: Exception) {
                ProductionLogger.recordException(e)
                errorMessage = "Risk engine connection failure: ${e.message ?: "Unknown error"}"
            } finally {
                isLoading = false
            }
        }
    }

    fun runStressTest(tickers: List<String>, weights: List<Double>, scenarioId: String) {
        viewModelScope.launch {
            isStressLoading = true
            try {
                stressTestResult = repository.runStressTest(tickers, weights, scenarioId)
            } catch (e: Exception) {
                ProductionLogger.recordException(e)
            } finally {
                isStressLoading = false
            }
        }
    }
}
