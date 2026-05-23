package com.example.splitbill.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitbill.data.GroupRepository
import com.example.splitbill.data.api.GroupResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface GroupListUiState {
  object Loading : GroupListUiState
  data class Success(val groups: List<GroupResponse>) : GroupListUiState
  data class Error(val message: String) : GroupListUiState
}

class GroupListViewModel(private val groupRepository: GroupRepository) : ViewModel() {
  private val _uiState = MutableStateFlow<GroupListUiState>(GroupListUiState.Loading)
  val uiState: StateFlow<GroupListUiState> = _uiState.asStateFlow()

  init {
    loadGroups()
  }

  fun loadGroups() {
    // Only show loading skeleton on initial fetch to avoid jarring flashes
    if (_uiState.value !is GroupListUiState.Success) {
      _uiState.value = GroupListUiState.Loading
    }
    viewModelScope.launch {
      val result = groupRepository.getGroups()
      if (result.isSuccess) {
        _uiState.value = GroupListUiState.Success(result.getOrDefault(emptyList()))
      } else {
        _uiState.value = GroupListUiState.Error(result.exceptionOrNull()?.message ?: "Lỗi tải nhóm")
      }
    }
  }

  fun createGroup(name: String) {
    viewModelScope.launch {
      val result = groupRepository.createGroup(name)
      if (result.isSuccess) {
        loadGroups() // Reload
      }
    }
  }
}
