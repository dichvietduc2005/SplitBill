package com.example.splitbill.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class TokenManager(private val context: Context) {

  private val encryptedPrefs: SharedPreferences by lazy {
    try {
      val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

      EncryptedSharedPreferences.create(
        context,
        "secure_token_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
      )
    } catch (e: Exception) {
      android.util.Log.e("TokenManager", "EncryptedSharedPreferences init failed, fallback to private prefs", e)
      context.getSharedPreferences("secure_token_prefs_fallback", Context.MODE_PRIVATE)
    }
  }

  private val _jwtFlow = MutableStateFlow<String?>(null)
  private val _biometricFlow = MutableStateFlow<String?>(null)

  init {
    val initialJwt = encryptedPrefs.getString(KEY_JWT, null)
    val initialBio = encryptedPrefs.getString(KEY_BIOMETRIC, null)
    _jwtFlow.value = initialJwt
    _biometricFlow.value = initialBio
    cachedToken = initialJwt

    // Di chuyển dữ liệu cũ từ DataStore không mã hóa (nếu có) sang EncryptedSharedPreferences
    if (initialJwt == null || initialBio == null) {
      CoroutineScope(Dispatchers.IO).launch {
        try {
          val oldPrefs = context.dataStore.data.firstOrNull()
          val oldJwt = oldPrefs?.get(LEGACY_JWT_KEY)
          val oldBio = oldPrefs?.get(LEGACY_BIOMETRIC_KEY)

          if (!oldJwt.isNullOrBlank() && encryptedPrefs.getString(KEY_JWT, null) == null) {
            saveToken(oldJwt)
          }
          if (!oldBio.isNullOrBlank() && encryptedPrefs.getString(KEY_BIOMETRIC, null) == null) {
            saveBiometricToken(oldBio)
          }
          if (oldJwt != null || oldBio != null) {
            context.dataStore.edit { prefs ->
              prefs.remove(LEGACY_JWT_KEY)
              prefs.remove(LEGACY_BIOMETRIC_KEY)
            }
          }
        } catch (_: Exception) {}
      }
    }
  }

  companion object {
    private const val KEY_JWT = "enc_jwt_token"
    private const val KEY_BIOMETRIC = "enc_biometric_token"
    private val LEGACY_JWT_KEY = stringPreferencesKey("jwt_token")
    private val LEGACY_BIOMETRIC_KEY = stringPreferencesKey("biometric_token")

    @Volatile private var cachedToken: String? = null

    fun getUserIdFromToken(token: String?): String? {
      if (token == null) return null
      return try {
        val rawJwt = if (token.trimStart().startsWith("{")) {
          org.json.JSONObject(token).optString("access_token")
        } else {
          token
        }
        val parts = rawJwt.split(".")
        if (parts.size != 3) return null
        val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
        val json = org.json.JSONObject(payload)
        if (json.has("sub")) json.getString("sub") else if (json.has("id")) json.getString("id") else null
      } catch (e: Exception) {
        null
      }
    }
  }

  fun getToken(): Flow<String?> = _jwtFlow.asStateFlow()

  fun getUserId(): String? = getUserIdFromToken(cachedToken ?: encryptedPrefs.getString(KEY_JWT, null))

  suspend fun getCachedToken(): String? {
    if (cachedToken != null) return cachedToken
    val token = encryptedPrefs.getString(KEY_JWT, null)
    cachedToken = token
    _jwtFlow.value = token
    return token
  }

  fun getBiometricToken(): Flow<String?> = _biometricFlow.asStateFlow()

  suspend fun saveToken(token: String) {
    cachedToken = token
    encryptedPrefs.edit().putString(KEY_JWT, token).apply()
    _jwtFlow.value = token
  }

  suspend fun saveBiometricToken(token: String) {
    encryptedPrefs.edit().putString(KEY_BIOMETRIC, token).apply()
    _biometricFlow.value = token
  }

  suspend fun deleteToken() {
    cachedToken = null
    encryptedPrefs.edit().remove(KEY_JWT).apply()
    _jwtFlow.value = null
  }

  suspend fun deleteBiometricToken() {
    encryptedPrefs.edit().remove(KEY_BIOMETRIC).apply()
    _biometricFlow.value = null
  }
}
