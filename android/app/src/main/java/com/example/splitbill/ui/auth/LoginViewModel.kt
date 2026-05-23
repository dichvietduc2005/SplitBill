package com.example.splitbill.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitbill.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LoginUiState {
  object Idle : LoginUiState
  object Loading : LoginUiState
  object Success : LoginUiState
  data class Error(val message: String) : LoginUiState
}

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
  private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
  val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

  private val _hasBiometricToken = MutableStateFlow(false)
  val hasBiometricToken: StateFlow<Boolean> = _hasBiometricToken.asStateFlow()

  init {
    checkLoginStatus()
    checkBiometricAvailability()
  }

  fun checkLoginStatus() {
    viewModelScope.launch {
      if (authRepository.isLoggedIn()) {
        _uiState.value = LoginUiState.Success
      } else {
        _uiState.value = LoginUiState.Idle
      }
    }
  }

  fun checkBiometricAvailability() {
    viewModelScope.launch {
      _hasBiometricToken.value = authRepository.hasBiometricToken()
    }
  }

  fun resetState() {
    _uiState.value = LoginUiState.Idle
  }

  fun login(username: String, password: String) {
    if (username.isBlank() || password.isBlank()) {
      _uiState.value = LoginUiState.Error("Vui lòng nhập đầy đủ thông tin")
      return
    }

    _uiState.value = LoginUiState.Loading
    viewModelScope.launch {
      val result = authRepository.login(username, password)
      if (result.isSuccess) {
        _uiState.value = LoginUiState.Success
      } else {
        _uiState.value = LoginUiState.Error(result.exceptionOrNull()?.message ?: "Đăng nhập thất bại")
      }
    }
  }

  fun register(username: String, password: String) {
    if (username.isBlank() || password.isBlank()) {
      _uiState.value = LoginUiState.Error("Vui lòng nhập đầy đủ thông tin")
      return
    }

    _uiState.value = LoginUiState.Loading
    viewModelScope.launch {
      val result = authRepository.register(username, password)
      if (result.isSuccess) {
        _uiState.value = LoginUiState.Success
      } else {
        _uiState.value = LoginUiState.Error(result.exceptionOrNull()?.message ?: "Đăng ký thất bại")
      }
    }
  }

  fun loginWithBiometrics() {
    _uiState.value = LoginUiState.Loading
    viewModelScope.launch {
      val success = authRepository.loginWithBiometrics()
      if (success) {
        _uiState.value = LoginUiState.Success
      } else {
        _uiState.value = LoginUiState.Error("Sinh trắc học không khớp hoặc chưa được thiết lập")
      }
    }
  }
}
