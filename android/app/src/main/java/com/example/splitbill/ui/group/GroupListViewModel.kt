package com.example.splitbill.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitbill.data.GroupRepository
import com.example.splitbill.data.InviteRepository
import com.example.splitbill.data.api.GroupResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface GroupListUiState {
  object Loading : GroupListUiState
  data class Success(val groups: List<GroupResponse>, val actionMessage: String? = null) : GroupListUiState
  data class Error(val message: String) : GroupListUiState
}

class GroupListViewModel(
  private val groupRepository: GroupRepository,
  private val inviteRepository: InviteRepository
) : ViewModel() {

  companion object {
    private var cachedState: GroupListUiState? = null
    fun clearCache() {
      cachedState = null
    }
  }

  private val _uiState = MutableStateFlow<GroupListUiState>(
    cachedState ?: GroupListUiState.Loading
  )
  val uiState: StateFlow<GroupListUiState> = _uiState.asStateFlow()

  init {
    loadGroups()
  }

  fun loadGroups(onComplete: (() -> Unit)? = null) {
    // Only show loading skeleton on initial fetch to avoid jarring flashes
    if (_uiState.value !is GroupListUiState.Success) {
      _uiState.value = GroupListUiState.Loading
    }
    viewModelScope.launch {
      try {
        val result = groupRepository.getGroups()
        if (result.isSuccess) {
          val newState = GroupListUiState.Success(result.getOrDefault(emptyList()))
          _uiState.value = newState
          cachedState = newState
        } else {
          _uiState.value = GroupListUiState.Error(result.exceptionOrNull()?.message ?: "Lỗi tải nhóm")
        }
      } finally {
        onComplete?.invoke()
      }
    }
  }

  fun createGroup(name: String) {
    viewModelScope.launch {
      val result = groupRepository.createGroup(name)
      if (result.isSuccess) {
        val currentState = _uiState.value
        if (currentState is GroupListUiState.Success) {
          _uiState.value = currentState.copy(actionMessage = "Đã tạo nhóm thành công")
        }
        loadGroups() // Reload
      } else {
        val errorMsg = result.exceptionOrNull()?.message ?: "Lỗi tạo nhóm"
        val currentState = _uiState.value
        if (currentState is GroupListUiState.Success) {
          _uiState.value = currentState.copy(actionMessage = "Lỗi: $errorMsg")
        } else {
          _uiState.value = GroupListUiState.Error(errorMsg)
        }
      }
    }
  }

  fun joinGroup(groupId: String) {
    viewModelScope.launch {
      val result = groupRepository.joinGroup(groupId)
      if (result.isSuccess) {
        val currentState = _uiState.value
        if (currentState is GroupListUiState.Success) {
          _uiState.value = currentState.copy(actionMessage = result.getOrNull() ?: "Đã tham gia nhóm")
        }
        loadGroups() // Reload
      } else {
        val errorMsg = result.exceptionOrNull()?.message ?: "Lỗi tham gia nhóm"
        val currentState = _uiState.value
        if (currentState is GroupListUiState.Success) {
          _uiState.value = currentState.copy(actionMessage = "Lỗi: $errorMsg")
        } else {
          _uiState.value = GroupListUiState.Error(errorMsg)
        }
      }
    }
  }

  fun joinByInvite(inviteCode: String) {
    viewModelScope.launch {
      val result = inviteRepository.joinByInvite(inviteCode)
      if (result.isSuccess) {
        val currentState = _uiState.value
        if (currentState is GroupListUiState.Success) {
          _uiState.value = currentState.copy(actionMessage = "Đã tham gia nhóm thành công")
        }
        loadGroups() // Reload
      } else {
        val errorMsg = result.exceptionOrNull()?.message ?: "Lỗi tham gia bằng mã mời"
        val currentState = _uiState.value
        if (currentState is GroupListUiState.Success) {
          _uiState.value = currentState.copy(actionMessage = "Lỗi: $errorMsg")
        } else {
          _uiState.value = GroupListUiState.Error(errorMsg)
        }
      }
    }
  }

  fun clearActionMessage() {
    val currentState = _uiState.value
    if (currentState is GroupListUiState.Success) {
      _uiState.value = currentState.copy(actionMessage = null)
    }
  }
}
