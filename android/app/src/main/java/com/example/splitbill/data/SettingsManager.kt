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
}
