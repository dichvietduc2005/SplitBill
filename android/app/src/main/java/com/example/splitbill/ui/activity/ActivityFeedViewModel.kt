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

  private val _uiState = MutableStateFlow<ActivityFeedUiState>(ActivityFeedUiState.Loading)
  val uiState: StateFlow<ActivityFeedUiState> = _uiState.asStateFlow()

  init {
    loadActivities()
  }

  fun loadActivities() {
    _uiState.value = ActivityFeedUiState.Loading
    viewModelScope.launch {
      val result = groupRepository.getActivities(groupId, limit = 100, offset = 0)
      if (result.isSuccess) {
        val paginatedResponse = result.getOrNull()!!
        _uiState.value = ActivityFeedUiState.Success(paginatedResponse.data)
      } else {
        _uiState.value = ActivityFeedUiState.Error(result.exceptionOrNull()?.message ?: "Lỗi tải lịch sử hoạt động")
      }
    }
  }
}
