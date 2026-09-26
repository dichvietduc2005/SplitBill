package com.example.splitbill.data

import com.example.splitbill.data.supabase.ProfileEntity
import com.example.splitbill.data.supabase.SupabaseConfig
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository(private val tokenManager: TokenManager) {
  
  private fun clearAllCaches() {
    com.example.splitbill.ui.group.GroupListViewModel.clearCache()
    com.example.splitbill.ui.profile.ProfileViewModel.clearCache()
    com.example.splitbill.ui.settings.SettingsViewModel.clearCache()
  }

  suspend fun login(emailOrUsername: String, password: String): Result<String> {
    return try {
      clearAllCaches()
      val email = if (emailOrUsername.contains("@")) emailOrUsername.trim() else "${emailOrUsername.trim()}@splitbill.app"
      val auth = SupabaseConfig.client.auth
      
      try {
        auth.signOut()
      } catch (_: Exception) {}
      
      auth.signInWith(Email) {
        this.email = email
        this.password = password
      }
      val token = auth.currentAccessTokenOrNull() 
        ?: auth.currentSessionOrNull()?.accessToken 
        ?: "supabase_session"
      
      tokenManager.saveToken(token)
      tokenManager.saveBiometricToken(token)

      // Đảm bảo profile tồn tại (tránh FK violation khi tạo nhóm)
      upsertProfile()

      Result.success(token)
    } catch (e: Exception) {
      Result.failure(Exception(e.message ?: "Đăng nhập thất bại"))
    }
  }

  suspend fun register(username: String, password: String, emailInput: String? = null): Result<String> {
    return try {
      clearAllCaches()
      try {
        SupabaseConfig.client.auth.signOut()
      } catch (_: Exception) {}
      tokenManager.deleteToken()
      tokenManager.deleteBiometricToken()

      val email = if (!emailInput.isNullOrBlank() && emailInput.contains("@")) {
        emailInput.trim()
      } else {
        "${username.trim()}@splitbill.app"
      }
      val auth = SupabaseConfig.client.auth
      auth.signUpWith(Email) {
        this.email = email
        this.password = password
        data = buildJsonObject {
          put("username", username.trim())
        }
      }

      // Nếu Supabase không tự động tạo session khi signUp, đăng nhập luôn
      if (auth.currentSessionOrNull() == null) {
        auth.signInWith(Email) {
          this.email = email
          this.password = password
        }
      }

      val token = auth.currentAccessTokenOrNull() 
        ?: auth.currentSessionOrNull()?.accessToken 
        ?: "supabase_session"

      tokenManager.saveToken(token)
      tokenManager.saveBiometricToken(token)

      // Đảm bảo profile tồn tại (trigger DB có thể bị chậm)
      upsertProfile()

      Result.success(token)
    } catch (e: Exception) {
      Result.failure(Exception(e.message ?: "Đăng ký thất bại"))
    }
  }

  suspend fun logout() {
    try {
      SupabaseConfig.client.auth.signOut()
    } catch (_: Exception) {}
    tokenManager.deleteToken()
    tokenManager.deleteBiometricToken()
    clearAllCaches()
  }

  suspend fun isLoggedIn(): Boolean {
    val session = SupabaseConfig.client.auth.currentSessionOrNull()
    if (session != null) return true
    val token = tokenManager.getToken().first()
    return !token.isNullOrBlank()
  }

  suspend fun getCurrentUserId(): String? {
    return SupabaseConfig.client.auth.currentUserOrNull()?.id
      ?: TokenManager.getUserIdFromToken(tokenManager.getCachedToken())
  }

  /**
   * Upsert profile cho user hiện tại vào bảng profiles.
   * Cần thiết vì DB trigger có thể bị chậm hoặc user cũ chưa có profile.
   */
  private suspend fun upsertProfile() {
    try {
      val auth = SupabaseConfig.client.auth
      val user = auth.currentUserOrNull() ?: return
      val username = user.userMetadata?.get("username")?.toString()
        ?.trim('"')
        ?: user.email?.substringBefore('@')
        ?: "user_${user.id.take(6)}"

      SupabaseConfig.client.postgrest["profiles"].upsert(
        ProfileEntity(
          id = user.id,
          email = user.email ?: "",
          username = username,
          avatarUrl = null
        )
      ) { onConflict = "id" }

      android.util.Log.d("SplitBill_Auth", "Profile upserted: ${user.id}")
    } catch (e: Exception) {
      // Không fail login vì lỗi upsert profile
      android.util.Log.w("SplitBill_Auth", "upsertProfile warning: ${e.message}")
    }
  }

  suspend fun hasBiometricToken(): Boolean {
    val bioToken = tokenManager.getBiometricToken().first()
    return !bioToken.isNullOrBlank()
  }

  suspend fun loginWithBiometrics(): Boolean {
    val bioToken = tokenManager.getBiometricToken().first()
    return if (!bioToken.isNullOrBlank()) {
      tokenManager.saveToken(bioToken)
      true
    } else {
      false
    }
  }
}
