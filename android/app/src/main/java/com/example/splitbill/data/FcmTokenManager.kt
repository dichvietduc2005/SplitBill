package com.example.splitbill.data

import com.example.splitbill.data.supabase.SupabaseConfig
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

class FcmTokenManager(private val tokenManager: TokenManager) {

  private val postgrest get() = SupabaseConfig.client.postgrest
  private val auth get() = SupabaseConfig.client.auth

  suspend fun registerToken(token: String): Result<Unit> {
    return try {
      val userId = auth.currentUserOrNull()?.id
      if (userId == null) {
        return Result.failure(Exception("Not logged in"))
      }
      postgrest["fcm_tokens"].upsert(
        mapOf("user_id" to userId, "token" to token, "device_type" to "android")
      )
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun unregisterToken(): Result<Unit> {
    return try {
      val userId = auth.currentUserOrNull()?.id ?: return Result.success(Unit)
      postgrest["fcm_tokens"].delete { filter { eq("user_id", userId) } }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
