package com.example.splitbill.data

import com.example.splitbill.data.api.ApiService
import com.example.splitbill.data.api.RegisterFcmTokenRequest
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.flow.first

class FcmTokenManager(private val tokenManager: TokenManager) {

  private suspend fun getClient() = ApiService.createClient(tokenManager.getToken().first())

  suspend fun registerToken(token: String): Result<Unit> {
    return try {
      val jwtToken = tokenManager.getToken().first()
      if (jwtToken.isNullOrBlank()) {
        return Result.failure(Exception("Not logged in"))
      }
      getClient().post("/api/fcm/register") {
        setBody(RegisterFcmTokenRequest(token))
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun unregisterToken(): Result<Unit> {
    return try {
      val jwtToken = tokenManager.getToken().first()
      if (jwtToken.isNullOrBlank()) {
        return Result.success(Unit)
      }
      getClient().delete("/api/fcm/unregister")
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
