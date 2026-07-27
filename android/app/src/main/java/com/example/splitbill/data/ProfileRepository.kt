package com.example.splitbill.data

import com.example.splitbill.data.api.ApiService
import com.example.splitbill.data.api.ProfileResponse
import com.example.splitbill.data.api.UpdateBankInfoRequest
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import kotlinx.coroutines.flow.first

class ProfileRepository(private val tokenManager: TokenManager) {

  private suspend fun getClient() = ApiService.createClient(tokenManager.getToken().first())

  /** Lấy profile của người dùng đang đăng nhập */
  suspend fun getMyProfile(): Result<ProfileResponse> {
    return try {
      val response: ProfileResponse = getClient().get("/api/profile").body()
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /** Lấy thông tin ngân hàng của một user khác để tạo QR code */
  suspend fun getUserProfile(userId: String): Result<ProfileResponse> {
    return try {
      val response: ProfileResponse = getClient().get("/api/profile/$userId").body()
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /** Cập nhật thông tin ngân hàng của mình */
  suspend fun updateBankInfo(
    bankCode: String,
    accountNumber: String,
    accountName: String
  ): Result<String> {
    return try {
      val response: Map<String, String> = getClient().put("/api/profile/bank") {
        setBody(UpdateBankInfoRequest(bankCode, accountNumber, accountName))
      }.body()
      Result.success(response["message"] ?: "Cập nhật thành công")
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /** Upload ảnh đại diện */
  suspend fun uploadAvatar(imageBytes: ByteArray): Result<String> {
    return try {
      val response: Map<String, String> = getClient().post("/api/profile/avatar") {
        setBody(
          io.ktor.client.request.forms.MultiPartFormDataContent(
            io.ktor.client.request.forms.formData {
              append(
                key = "file",
                value = imageBytes,
                headers = io.ktor.http.Headers.build {
                  append(io.ktor.http.HttpHeaders.ContentType, "image/jpeg")
                  append(io.ktor.http.HttpHeaders.ContentDisposition, "filename=\"avatar.jpg\"")
                }
              )
            }
          )
        )
      }.body()
      Result.success(response["avatarUrl"] ?: "")
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
