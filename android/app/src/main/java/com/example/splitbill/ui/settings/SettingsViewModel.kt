package com.example.splitbill.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitbill.data.AuthRepository
import com.example.splitbill.data.ProfileRepository
import com.example.splitbill.data.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsProfileUiState {
  object Loading : SettingsProfileUiState
  data class Success(val username: String, val email: String) : SettingsProfileUiState
  object Error : SettingsProfileUiState
}

class SettingsViewModel(
  private val settingsManager: SettingsManager,
  private val authRepository: AuthRepository,
  private val profileRepository: ProfileRepository
) : ViewModel() {

  private val _profileUiState = MutableStateFlow<SettingsProfileUiState>(SettingsProfileUiState.Loading)
  val profileUiState: StateFlow<SettingsProfileUiState> = _profileUiState.asStateFlow()

  val themeMode: StateFlow<String> = settingsManager.themeMode
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = "system"
    )

  val fontScale: StateFlow<Float> = settingsManager.fontScale
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = 1.0f
    )

  val language: StateFlow<String> = settingsManager.language
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = "vi"
    )

  val biometricEnabled: StateFlow<Boolean> = settingsManager.biometricEnabled
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = false
    )

  val pushEnabled: StateFlow<Boolean> = settingsManager.pushEnabled
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = true
    )

  init {
    loadProfile()
  }

  fun loadProfile() {
    viewModelScope.launch {
      val result = profileRepository.getMyProfile()
      if (result.isSuccess) {
        val profile = result.getOrNull()!!
        _profileUiState.value = SettingsProfileUiState.Success(profile.username, profile.email)
      } else {
        _profileUiState.value = SettingsProfileUiState.Error
      }
    }
  }

  fun saveThemeMode(mode: String) {
    viewModelScope.launch {
      settingsManager.saveThemeMode(mode)
    }
  }

  fun saveFontScale(scale: Float) {
    viewModelScope.launch {
      settingsManager.saveFontScale(scale)
    }
  }

  fun saveLanguage(langCode: String) {
    viewModelScope.launch {
      settingsManager.saveLanguage(langCode)
    }
  }

  fun saveBiometricEnabled(enabled: Boolean) {
    viewModelScope.launch {
      settingsManager.saveBiometricEnabled(enabled)
    }
  }

  fun savePushEnabled(enabled: Boolean) {
    viewModelScope.launch {
      settingsManager.savePushEnabled(enabled)
    }
  }

  fun logout(onSuccess: () -> Unit) {
    viewModelScope.launch {
      authRepository.logout()
      onSuccess()
    }
  }
}
