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
  fun getContext(): Context = context

  private val encryptedPrefs: SharedPreferences? by lazy {
    try {
      createEncryptedPrefs()
    } catch (e: Exception) {
      // Khi KeyStore hoặc cấu hình mã hóa lỗi, không tự ý xóa file SharedPreferences để tránh mất dữ liệu không thể khôi phục.
      // Báo lỗi rõ ràng và từ chối hoạt động mã hóa không an toàn.
      android.util.Log.e("TokenManager", "EncryptedSharedPreferences initialization failed securely", e)
      null
    }
  }

  private fun createEncryptedPrefs(): SharedPreferences {
    val masterKey = MasterKey.Builder(context)
      .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
      .build()

    return EncryptedSharedPreferences.create(
      context,
      "secure_token_prefs",
      masterKey,
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
  }

  private val _jwtFlow = MutableStateFlow<String?>(null)
  private val _biometricFlow = MutableStateFlow<String?>(null)

  init {
    val initialJwt = encryptedPrefs?.getString(KEY_JWT, null)
    val initialBio = encryptedPrefs?.getString(KEY_BIOMETRIC, null)
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

          var jwtMigrated = false
          if (!oldJwt.isNullOrBlank() && encryptedPrefs?.getString(KEY_JWT, null) == null) {
            saveToken(oldJwt)
            // Kiểm tra xem đã ghi thành công vào EncryptedSharedPreferences chưa
            jwtMigrated = encryptedPrefs?.getString(KEY_JWT, null) == oldJwt
          } else if (oldJwt.isNullOrBlank()) {
            jwtMigrated = true
          }

          var bioMigrated = false
          if (!oldBio.isNullOrBlank() && encryptedPrefs?.getString(KEY_BIOMETRIC, null) == null) {
            saveBiometricToken(oldBio)
            bioMigrated = encryptedPrefs?.getString(KEY_BIOMETRIC, null) == oldBio
          } else if (oldBio.isNullOrBlank()) {
            bioMigrated = true
          }

          // Chỉ xóa bản ghi ở DataStore cũ khi và chỉ khi bản mới đã lưu an toàn thành công
          if (jwtMigrated && bioMigrated) {
            context.dataStore.edit { prefs ->
              if (!oldJwt.isNullOrBlank()) prefs.remove(LEGACY_JWT_KEY)
              if (!oldBio.isNullOrBlank()) prefs.remove(LEGACY_BIOMETRIC_KEY)
            }
          }
        } catch (e: Exception) {
          android.util.Log.e("TokenManager", "Migration from DataStore failed, retaining old data", e)
        }
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

    fun getUsernameFromToken(token: String?): String? {
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
        val userMetadata = json.optJSONObject("user_metadata")
        val metaName = userMetadata?.optString("username")?.takeIf { it.isNotBlank() }
        if (!metaName.isNullOrBlank()) return metaName
        val email = json.optString("email").takeIf { it.isNotBlank() }
        email?.substringBefore('@')
      } catch (e: Exception) {
        null
      }
    }

    fun isTokenExpired(token: String?): Boolean {
      if (token.isNullOrBlank()) return true
      return try {
        val rawJwt = if (token.trimStart().startsWith("{")) {
          val json = org.json.JSONObject(token)
          val refreshToken = json.optString("refresh_token")
          if (refreshToken.isNotBlank()) {
            return false // Còn refresh_token để làm mới phiên qua Supabase
          }
          json.optString("access_token")
        } else {
          token
        }
        val parts = rawJwt.split(".")
        if (parts.size != 3) return true
        val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
        val json = org.json.JSONObject(payload)
        val exp = json.optLong("exp", 0L)
        if (exp == 0L) return false
        (exp * 1000) <= System.currentTimeMillis()
      } catch (e: Exception) {
        false
      }
    }
  }

  fun getToken(): Flow<String?> = _jwtFlow.asStateFlow()

  fun getUserId(): String? = getUserIdFromToken(cachedToken ?: encryptedPrefs?.getString(KEY_JWT, null))

  fun hasValidToken(): Boolean {
    val token = cachedToken ?: encryptedPrefs?.getString(KEY_JWT, null)
    if (token.isNullOrBlank() || getUserId().isNullOrBlank()) return false
    return !isTokenExpired(token)
  }

  suspend fun getCachedToken(): String? {
    if (cachedToken != null) return cachedToken
    val token = encryptedPrefs?.getString(KEY_JWT, null)
    cachedToken = token
    _jwtFlow.value = token
    return token
  }

  fun getBiometricToken(): Flow<String?> = _biometricFlow.asStateFlow()

  suspend fun saveToken(token: String) {
    cachedToken = token
    encryptedPrefs?.edit()?.putString(KEY_JWT, token)?.apply()
    _jwtFlow.value = token
  }

  suspend fun saveBiometricToken(token: String) {
    encryptedPrefs?.edit()?.putString(KEY_BIOMETRIC, token)?.apply()
    _biometricFlow.value = token
  }

  suspend fun deleteToken() {
    cachedToken = null
    encryptedPrefs?.edit()?.remove(KEY_JWT)?.apply()
    _jwtFlow.value = null
  }

  suspend fun deleteBiometricToken() {
    encryptedPrefs?.edit()?.remove(KEY_BIOMETRIC)?.apply()
    _biometricFlow.value = null
  }
}
