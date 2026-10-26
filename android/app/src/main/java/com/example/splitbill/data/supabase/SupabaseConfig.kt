package com.example.splitbill.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

object SupabaseConfig {
    const val SUPABASE_URL = "https://qvqcvwepxluzatmzgiix.supabase.co"
    
    // Khóa anon public từ Supabase Dashboard (đọc từ local.properties qua BuildConfig)
    var supabaseAnonKey: String = com.example.splitbill.BuildConfig.SUPABASE_ANON_KEY

    private var _client: SupabaseClient? = null
    private val sessionMutex = Mutex()

    val client: SupabaseClient
        get() {
            if (_client == null) {
                _client = createSupabaseClient(
                    supabaseUrl = SUPABASE_URL,
                    supabaseKey = supabaseAnonKey
                ) {
                    install(Auth) {
                        alwaysAutoRefresh = true      // SDK tự động refresh khi gần hết hạn
                        autoLoadFromStorage = false   // ta tự quản lý lưu/đọc token
                        autoSaveToStorage = false
                    }
                    install(Postgrest)
                    install(Storage)
                    install(Realtime)
                }
            }
            return _client!!
        }

    /**
     * Đảm bảo session hợp lệ:
     * 1. Mutex đồng bộ hóa để tránh nhiều coroutine refresh đồng thời làm Supabase hủy token ("refresh_token_already_used")
     * 2. Nếu chưa có session → import từ token đã lưu (gồm cả refresh_token)
     * 3. Nếu session đã hết hạn (hoặc sắp hết hạn trong 60s) → gọi refreshCurrentSession()
     * 4. Sau refresh, lưu token mới lại vào TokenManager
     */
    suspend fun ensureSession(token: String?, tokenManager: com.example.splitbill.data.TokenManager? = null): Boolean {
        if (token.isNullOrBlank()) return false
        return sessionMutex.withLock {
            try {
                val auth = client.auth
                val now = Clock.System.now()

                // Parse access + refresh token
                var accessToken = ""
                var refreshToken = ""
                if (token.startsWith("{")) {
                    val json = org.json.JSONObject(token)
                    accessToken = json.optString("access_token")
                    refreshToken = json.optString("refresh_token")
                } else {
                    accessToken = token
                }

                var currentSession = auth.currentSessionOrNull()
                if (currentSession == null) {
                    // Chưa có session → import
                    if (accessToken.isNotBlank() && refreshToken.isNotBlank()) {
                        auth.importAuthToken(accessToken, refreshToken)
                    } else if (accessToken.isNotBlank()) {
                        auth.importAuthToken(accessToken)
                    }
                    currentSession = auth.currentSessionOrNull()
                }

                // Kiểm tra xem session hiện tại có hết hạn hoặc sắp hết hạn (< 60s) không
                val isExpiredOrExpiring = currentSession != null && currentSession.expiresAt <= now.plus(60.seconds)

                if (isExpiredOrExpiring) {
                    val effectiveRefreshToken = currentSession?.refreshToken?.takeIf { it.isNotBlank() } ?: refreshToken
                    if (effectiveRefreshToken.isNotBlank()) {
                        try {
                            auth.refreshCurrentSession()
                            val newSession = auth.currentSessionOrNull()
                            if (newSession != null && tokenManager != null) {
                                val newJson = org.json.JSONObject()
                                newJson.put("access_token", newSession.accessToken)
                                newJson.put("refresh_token", newSession.refreshToken)
                                tokenManager.saveToken(newJson.toString())
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("SupabaseConfig", "Refresh session failed: ${e.message}")
                            // Nếu refresh thất bại và access_token đã hết hạn, phiên hoàn toàn không hợp lệ
                            if (currentSession != null && currentSession.expiresAt <= now) {
                                tokenManager?.deleteToken()
                                try { auth.signOut() } catch (_: Exception) {}
                                return@withLock false
                            }
                            val err = e.message ?: ""
                            if (err.contains("already_used", ignoreCase = true) ||
                                err.contains("revoked", ignoreCase = true) ||
                                err.contains("invalid", ignoreCase = true) ||
                                err.contains("expired", ignoreCase = true)) {
                                tokenManager?.deleteToken()
                                try { auth.signOut() } catch (_: Exception) {}
                                return@withLock false
                            }
                        }
                    } else if (currentSession != null && currentSession.expiresAt <= now) {
                        tokenManager?.deleteToken()
                        try { auth.signOut() } catch (_: Exception) {}
                        return@withLock false
                    }
                }

                val finalSession = auth.currentSessionOrNull()
                if (finalSession == null || finalSession.expiresAt <= now) {
                    tokenManager?.deleteToken()
                    return@withLock false
                }
                true
            } catch (e: Exception) {
                android.util.Log.e("SupabaseConfig", "ensureSession error: ${e.message}")
                tokenManager?.deleteToken()
                false
            }
        }
    }

    fun isAuthError(e: Throwable?): Boolean {
        if (e == null) return false
        val msg = e.message ?: ""
        return msg.contains("401", ignoreCase = true) ||
               msg.contains("JWT expired", ignoreCase = true) ||
               msg.contains("invalid JWT", ignoreCase = true) ||
               (msg.contains("token", ignoreCase = true) && (msg.contains("expired", ignoreCase = true) || msg.contains("invalid", ignoreCase = true))) ||
               msg.contains("PGRST301", ignoreCase = true) ||
               msg.contains("unauthorized", ignoreCase = true) ||
               msg.contains("Chưa đăng nhập", ignoreCase = true) ||
               msg.contains("hết hạn", ignoreCase = true) ||
               msg.contains("invalid_grant", ignoreCase = true) ||
               (msg.contains("session", ignoreCase = true) && msg.contains("expired", ignoreCase = true))
    }
}

