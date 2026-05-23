package com.example.splitbill.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class TokenManager(private val context: Context) {
  companion object {
    val JWT_TOKEN_KEY = stringPreferencesKey("jwt_token")
    val BIOMETRIC_TOKEN_KEY = stringPreferencesKey("biometric_token")
  }

  fun getToken(): Flow<String?> {
    return context.dataStore.data.map { preferences ->
      preferences[JWT_TOKEN_KEY]
    }
  }

  fun getBiometricToken(): Flow<String?> {
    return context.dataStore.data.map { preferences ->
      preferences[BIOMETRIC_TOKEN_KEY]
    }
  }

  suspend fun saveToken(token: String) {
    context.dataStore.edit { preferences ->
      preferences[JWT_TOKEN_KEY] = token
    }
  }

  suspend fun saveBiometricToken(token: String) {
    context.dataStore.edit { preferences ->
      preferences[BIOMETRIC_TOKEN_KEY] = token
    }
  }

  suspend fun deleteToken() {
    context.dataStore.edit { preferences ->
      preferences.remove(JWT_TOKEN_KEY)
    }
  }

  suspend fun deleteBiometricToken() {
    context.dataStore.edit { preferences ->
      preferences.remove(BIOMETRIC_TOKEN_KEY)
    }
  }
}
