package com.example.splitbill.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

object SupabaseConfig {
    const val SUPABASE_URL = "https://qvqcvwepxluzatmzgiix.supabase.co"
    
    // Khóa anon public từ Supabase Dashboard (đọc từ local.properties qua BuildConfig)
    var supabaseAnonKey: String = com.example.splitbill.BuildConfig.SUPABASE_ANON_KEY

    private var _client: SupabaseClient? = null

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
     * 1. Nếu chưa có session → import từ token đã lưu (gồm cả refresh_token)
     * 2. Nếu session đã hết hạn (hoặc sắp hết hạn trong 60s) → gọi refreshCurrentSession()
     * 3. Sau refresh, lưu token mới lại vào TokenManager
     */
    suspend fun ensureSession(token: String?, tokenManager: com.example.splitbill.data.TokenManager? = null) {
        if (token.isNullOrBlank()) return
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

            val currentSession = auth.currentSessionOrNull()
            if (currentSession == null) {
                // Chưa có session → import
                if (accessToken.isNotBlank() && refreshToken.isNotBlank()) {
                    auth.importAuthToken(accessToken, refreshToken)
                } else if (accessToken.isNotBlank()) {
                    auth.importAuthToken(accessToken)
                }
            }

            // Kiểm tra xem session hiện tại có hết hạn hoặc sắp hết hạn (< 60s) không
            val session = auth.currentSessionOrNull()
            val isExpiredOrExpiring = session != null && session.expiresAt <= now.plus(60.seconds)

            if (isExpiredOrExpiring) {
                // Nếu có refresh token → refresh session
                if (refreshToken.isNotBlank() || session?.refreshToken?.isNotBlank() == true) {
                    try {
                        auth.refreshCurrentSession()
                        // Lưu token mới lại
                        val newSession = auth.currentSessionOrNull()
                        if (newSession != null && tokenManager != null) {
                            val newJson = org.json.JSONObject()
                            newJson.put("access_token", newSession.accessToken)
                            newJson.put("refresh_token", newSession.refreshToken)
                            tokenManager.saveToken(newJson.toString())
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("SupabaseConfig", "Refresh session failed: ${e.message}")
                        // Nếu refresh thất bại, thử re-import với token cũ
                        if (accessToken.isNotBlank() && refreshToken.isNotBlank()) {
                            try { auth.importAuthToken(accessToken, refreshToken) } catch (_: Exception) {}
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SupabaseConfig", "ensureSession error: ${e.message}")
        }
    }
}

