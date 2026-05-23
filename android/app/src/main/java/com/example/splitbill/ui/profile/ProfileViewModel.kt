package com.example.splitbill.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitbill.data.ProfileRepository
import com.example.splitbill.data.api.ProfileResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
  object Loading : ProfileUiState
  data class Success(val profile: ProfileResponse) : ProfileUiState
  data class Error(val message: String) : ProfileUiState
}

class ProfileViewModel(
  private val profileRepository: ProfileRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
  val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

  private val _saveState = MutableStateFlow<String?>(null)
  val saveState: StateFlow<String?> = _saveState.asStateFlow()

  init {
    loadProfile()
  }

  fun loadProfile() {
    _uiState.value = ProfileUiState.Loading
    viewModelScope.launch {
      val result = profileRepository.getMyProfile()
      _uiState.value = if (result.isSuccess) {
        ProfileUiState.Success(result.getOrNull()!!)
      } else {
        ProfileUiState.Error(result.exceptionOrNull()?.message ?: "Lỗi tải profile")
      }
    }
  }

  fun saveBankInfo(bankCode: String, accountNumber: String, accountName: String) {
    viewModelScope.launch {
      val result = profileRepository.updateBankInfo(bankCode, accountNumber, accountName)
      if (result.isSuccess) {
        _saveState.value = "success"
        loadProfile() // Reload để cập nhật UI
      } else {
        _saveState.value = "error:${result.exceptionOrNull()?.message}"
      }
    }
  }

  fun clearSaveState() {
    _saveState.value = null
  }
}
