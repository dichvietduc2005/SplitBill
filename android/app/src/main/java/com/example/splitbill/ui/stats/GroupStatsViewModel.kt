package com.example.splitbill.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitbill.data.StatsRepository
import com.example.splitbill.data.api.GroupStatsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface GroupStatsUiState {
  object Loading : GroupStatsUiState
  data class Success(val data: GroupStatsResponse) : GroupStatsUiState
  data class Error(val message: String) : GroupStatsUiState
}

class GroupStatsViewModel(
  private val groupId: String,
  private val statsRepository: StatsRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow<GroupStatsUiState>(GroupStatsUiState.Loading)
  val uiState: StateFlow<GroupStatsUiState> = _uiState.asStateFlow()

  init {
    loadGroupStats()
  }

  fun loadGroupStats() {
    _uiState.value = GroupStatsUiState.Loading
    viewModelScope.launch {
      val result = statsRepository.getGroupStats(groupId)
      if (result.isSuccess) {
        _uiState.value = GroupStatsUiState.Success(result.getOrNull()!!)
      } else {
        _uiState.value = GroupStatsUiState.Error(result.exceptionOrNull()?.message ?: "Lỗi tải thống kê nhóm")
      }
    }
  }
}
