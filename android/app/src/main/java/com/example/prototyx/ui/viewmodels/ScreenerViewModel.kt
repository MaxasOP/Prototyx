package com.example.prototyx.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.model.ScreenerResultItem
import com.example.prototyx.data.model.UserExperienceMode
import com.example.prototyx.util.ProductionLogger
import kotlinx.coroutines.launch

class ScreenerViewModel(private val repository: DataRepository) : ViewModel() {

    var queryText by mutableStateOf("")
    var userMode by mutableStateOf(UserExperienceMode.PRO)
    var results by mutableStateOf<List<ScreenerResultItem>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    fun performSearch(query: String? = null) {
        val q = query ?: queryText
        viewModelScope.launch {
            isLoading = true
            try {
                results = repository.searchScreener(q)
            } catch (e: Exception) {
                ProductionLogger.recordException(e)
            } finally {
                isLoading = false
            }
        }
    }
}
