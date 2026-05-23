package com.example.splitbill.data

import com.example.splitbill.data.api.ApiService
import com.example.splitbill.data.api.AuthResponse
import com.example.splitbill.data.api.LoginRequest
import com.example.splitbill.data.api.RegisterRequest
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.flow.first

class AuthRepository(private val tokenManager: TokenManager) {
  
  suspend fun login(username: String, password: String): Result<String> {
    return try {
      val client = ApiService.createClient() // No token for login
      val response: AuthResponse = client.post("/api/auth/login") {
        setBody(LoginRequest(username, password))
      }.body()
      
      tokenManager.saveToken(response.token)
      tokenManager.saveBiometricToken(response.token)
      Result.success(response.token)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun register(username: String, password: String): Result<String> {
    return try {
      val client = ApiService.createClient()
      val response: AuthResponse = client.post("/api/auth/register") {
        setBody(RegisterRequest(username, "$username@example.com", password))
      }.body()
      
      tokenManager.saveToken(response.token)
      tokenManager.saveBiometricToken(response.token)
      Result.success(response.token)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun logout() {
    tokenManager.deleteToken()
  }

  suspend fun isLoggedIn(): Boolean {
    val token = tokenManager.getToken().first()
    return !token.isNullOrBlank()
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
