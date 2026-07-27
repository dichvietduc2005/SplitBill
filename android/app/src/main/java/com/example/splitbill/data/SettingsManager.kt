package com.example.splitbill.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsManager(private val context: Context) {
  companion object {
    val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    val FONT_SCALE_KEY = floatPreferencesKey("font_scale")
    val LANGUAGE_KEY = stringPreferencesKey("language")
    val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
    val PUSH_ENABLED_KEY = booleanPreferencesKey("push_enabled")
    val AUTO_SETTLE_ENABLED_KEY = booleanPreferencesKey("auto_settle_enabled")
    val HAPTIC_ENABLED_KEY = booleanPreferencesKey("haptic_enabled")
    val SOUND_ENABLED_KEY = booleanPreferencesKey("sound_enabled")
    val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
  }

  val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
    prefs[THEME_MODE_KEY] ?: "system"
  }

  val fontScale: Flow<Float> = context.dataStore.data.map { prefs ->
    prefs[FONT_SCALE_KEY] ?: 1.0f
  }

  val language: Flow<String> = context.dataStore.data.map { prefs ->
    prefs[LANGUAGE_KEY] ?: "vi"
  }

  val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[BIOMETRIC_ENABLED_KEY] ?: false
  }

  val pushEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[PUSH_ENABLED_KEY] ?: true
  }

  val autoSettleEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[AUTO_SETTLE_ENABLED_KEY] ?: false
  }

  val hapticEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[HAPTIC_ENABLED_KEY] ?: true
  }

  val soundEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[SOUND_ENABLED_KEY] ?: true
  }

  val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[ONBOARDING_COMPLETED_KEY] ?: false
  }

  suspend fun saveThemeMode(mode: String) {
    context.dataStore.edit { prefs ->
      prefs[THEME_MODE_KEY] = mode
    }
  }

  suspend fun saveFontScale(scale: Float) {
    context.dataStore.edit { prefs ->
      prefs[FONT_SCALE_KEY] = scale
    }
  }

  suspend fun saveLanguage(langCode: String) {
    context.dataStore.edit { prefs ->
      prefs[LANGUAGE_KEY] = langCode
    }
  }

  suspend fun saveBiometricEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[BIOMETRIC_ENABLED_KEY] = enabled
    }
  }

  suspend fun savePushEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[PUSH_ENABLED_KEY] = enabled
    }
  }

  suspend fun saveAutoSettleEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[AUTO_SETTLE_ENABLED_KEY] = enabled
    }
  }

  suspend fun saveHapticEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[HAPTIC_ENABLED_KEY] = enabled
    }
  }

  suspend fun saveSoundEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[SOUND_ENABLED_KEY] = enabled
    }
  }

  suspend fun saveOnboardingCompleted(completed: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[ONBOARDING_COMPLETED_KEY] = completed
    }
  }
}
