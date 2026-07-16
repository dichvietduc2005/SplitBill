package com.example.splitbill.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitbill.data.StatsRepository
import com.example.splitbill.data.api.UserStatsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface StatsUiState {
  object Loading : StatsUiState
  data class Success(val data: UserStatsResponse) : StatsUiState
  data class Error(val message: String) : StatsUiState
}

class StatsViewModel(private val statsRepository: StatsRepository) : ViewModel() {

  private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
  val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

  init {
    loadStats()
  }

  fun loadStats() {
    // Keep data if already loaded to avoid blank screen flicker
    if (_uiState.value !is StatsUiState.Success) {
      _uiState.value = StatsUiState.Loading
    }
    viewModelScope.launch {
      val result = statsRepository.getUserStats()
      if (result.isSuccess) {
        _uiState.value = StatsUiState.Success(result.getOrNull()!!)
      } else {
        _uiState.value = StatsUiState.Error(result.exceptionOrNull()?.message ?: "Lỗi tải thống kê")
      }
    }
  }
}
