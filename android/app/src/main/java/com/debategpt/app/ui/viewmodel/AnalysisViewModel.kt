package com.debategpt.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.debategpt.app.data.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnalysisUiState(
    val isLoading: Boolean = false,
    val analysisSuccess: Boolean = false,
    val sentencesAnalyzed: Int? = null,
    val error: String? = null
)

data class WinnerUiState(
    val isLoading: Boolean = false,
    val winner: String? = null,
    val scores: Map<String, Double>? = null,
    val error: String? = null
)

class AnalysisViewModel : ViewModel() {

    private val _analysisState = MutableStateFlow(AnalysisUiState())
    val analysisState: StateFlow<AnalysisUiState> = _analysisState.asStateFlow()

    private val _winnerState = MutableStateFlow(WinnerUiState())
    val winnerState: StateFlow<WinnerUiState> = _winnerState.asStateFlow()

    fun analyzeStt() {
        viewModelScope.launch {
            _analysisState.value = _analysisState.value.copy(isLoading = true, error = null)
            try {
                val response = ApiClient.api.analyzeStt()
                if (response.isSuccessful) {
                    val body = response.body()
                    _analysisState.value = _analysisState.value.copy(
                        isLoading = false,
                        analysisSuccess = true,
                        sentencesAnalyzed = body?.sentences_analyzed
                    )
                } else {
                    _analysisState.value = _analysisState.value.copy(
                        isLoading = false,
                        error = response.message() ?: "Analysis failed"
                    )
                }
            } catch (e: Exception) {
                _analysisState.value = _analysisState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Network error"
                )
            }
        }
    }

    fun analyzeChatbot() {
        viewModelScope.launch {
            _analysisState.value = _analysisState.value.copy(isLoading = true, error = null)
            try {
                val response = ApiClient.api.analyzeChatbot()
                if (response.isSuccessful) {
                    val body = response.body()
                    _analysisState.value = _analysisState.value.copy(
                        isLoading = false,
                        analysisSuccess = true,
                        sentencesAnalyzed = body?.sentences_analyzed
                    )
                } else {
                    _analysisState.value = _analysisState.value.copy(
                        isLoading = false,
                        error = response.message() ?: "Analysis failed"
                    )
                }
            } catch (e: Exception) {
                _analysisState.value = _analysisState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Network error"
                )
            }
        }
    }

    fun getWinnerStt() {
        viewModelScope.launch {
            _winnerState.value = _winnerState.value.copy(isLoading = true, error = null)
            try {
                val response = ApiClient.api.winnerStt()
                if (response.isSuccessful) {
                    val data = response.body()?.data
                    _winnerState.value = _winnerState.value.copy(
                        isLoading = false,
                        winner = data?.winner,
                        scores = data?.scores
                    )
                } else {
                    _winnerState.value = _winnerState.value.copy(
                        isLoading = false,
                        error = response.body()?.toString() ?: response.message() ?: "Failed"
                    )
                }
            } catch (e: Exception) {
                _winnerState.value = _winnerState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Network error"
                )
            }
        }
    }

    fun getWinnerChatbot() {
        viewModelScope.launch {
            _winnerState.value = _winnerState.value.copy(isLoading = true, error = null)
            try {
                val response = ApiClient.api.winnerChatbot()
                if (response.isSuccessful) {
                    val data = response.body()?.data
                    _winnerState.value = _winnerState.value.copy(
                        isLoading = false,
                        winner = data?.winner,
                        scores = data?.scores
                    )
                } else {
                    _winnerState.value = _winnerState.value.copy(
                        isLoading = false,
                        error = response.message() ?: "Failed"
                    )
                }
            } catch (e: Exception) {
                _winnerState.value = _winnerState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Network error"
                )
            }
        }
    }

    fun resetAnalysisState() {
        _analysisState.value = AnalysisUiState()
    }

    fun resetWinnerState() {
        _winnerState.value = WinnerUiState()
    }
}
