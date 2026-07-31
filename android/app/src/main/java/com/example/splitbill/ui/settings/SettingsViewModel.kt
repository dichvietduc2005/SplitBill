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

  val autoSettleEnabled: StateFlow<Boolean> = settingsManager.autoSettleEnabled
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = false
    )

  val hapticEnabled: StateFlow<Boolean> = settingsManager.hapticEnabled
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = true
    )

  val soundEnabled: StateFlow<Boolean> = settingsManager.soundEnabled
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = true
    )

  companion object {
    private var cachedProfileState: SettingsProfileUiState? = null
  }

  init {
    // Khôi phục từ cache nếu có
    cachedProfileState?.let { _profileUiState.value = it }
    loadProfile()
  }

  fun loadProfile() {
    viewModelScope.launch {
      val result = profileRepository.getMyProfile()
      if (result.isSuccess) {
        val profile = result.getOrNull()!!
        val newState = SettingsProfileUiState.Success(profile.username, profile.email)
        _profileUiState.value = newState
        cachedProfileState = newState
      } else if (_profileUiState.value is SettingsProfileUiState.Loading) {
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

  fun saveAutoSettleEnabled(enabled: Boolean) {
    viewModelScope.launch {
      settingsManager.saveAutoSettleEnabled(enabled)
    }
  }

  fun saveHapticEnabled(enabled: Boolean) {
    viewModelScope.launch {
      settingsManager.saveHapticEnabled(enabled)
    }
  }

  fun saveSoundEnabled(enabled: Boolean) {
    viewModelScope.launch {
      settingsManager.saveSoundEnabled(enabled)
    }
  }

  fun logout(onSuccess: () -> Unit) {
    viewModelScope.launch {
      authRepository.logout()
      onSuccess()
    }
  }
}
