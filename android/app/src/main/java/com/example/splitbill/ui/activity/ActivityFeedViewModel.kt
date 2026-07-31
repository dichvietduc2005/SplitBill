package com.example.splitbill.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitbill.data.GroupRepository
import com.example.splitbill.data.api.ActivityResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ActivityFeedUiState {
  object Loading : ActivityFeedUiState
  data class Success(val activities: List<ActivityResponse>) : ActivityFeedUiState
  data class Error(val message: String) : ActivityFeedUiState
}

class ActivityFeedViewModel(
  private val groupRepository: GroupRepository,
  private val groupId: String
) : ViewModel() {

  companion object {
    private val stateCache = mutableMapOf<String, ActivityFeedUiState>()
  }

  private val _uiState = MutableStateFlow<ActivityFeedUiState>(
    stateCache[groupId] ?: ActivityFeedUiState.Loading
  )
  val uiState: StateFlow<ActivityFeedUiState> = _uiState.asStateFlow()

  init {
    loadActivities()
  }

  fun loadActivities() {
    if (_uiState.value !is ActivityFeedUiState.Success) {
      _uiState.value = ActivityFeedUiState.Loading
    }
    viewModelScope.launch {
      val result = groupRepository.getActivities(groupId, limit = 100, offset = 0)
      val newState = if (result.isSuccess) {
        val paginatedResponse = result.getOrNull()!!
        ActivityFeedUiState.Success(paginatedResponse.data)
      } else {
        if (_uiState.value is ActivityFeedUiState.Success) {
          _uiState.value
        } else {
          ActivityFeedUiState.Error(result.exceptionOrNull()?.message ?: "Lỗi tải lịch sử hoạt động")
        }
      }
      _uiState.value = newState
      if (newState is ActivityFeedUiState.Success) {
        stateCache[groupId] = newState
      }
    }
  }
}
