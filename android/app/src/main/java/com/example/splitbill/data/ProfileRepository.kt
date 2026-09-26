package com.example.splitbill.data

import com.example.splitbill.data.api.ProfileResponse
import com.example.splitbill.data.supabase.ProfileEntity
import com.example.splitbill.data.supabase.SupabaseConfig
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import java.util.UUID

class ProfileRepository(private val tokenManager: TokenManager) {

  private val postgrest get() = SupabaseConfig.client.postgrest
  private val storage get() = SupabaseConfig.client.storage
  private val auth get() = SupabaseConfig.client.auth

  /** Lấy profile của người dùng đang đăng nhập */
  suspend fun getMyProfile(): Result<ProfileResponse> {
    return try {
      val userId = auth.currentUserOrNull()?.id
        ?: TokenManager.getUserIdFromToken(tokenManager.getCachedToken())
        ?: return Result.failure(Exception("Chưa đăng nhập"))

      val profile = postgrest["profiles"]
        .select { filter { eq("id", userId) } }
        .decodeSingle<ProfileEntity>()

      Result.success(
        ProfileResponse(
          id = profile.id,
          username = profile.username,
          email = profile.email,
          avatarUrl = profile.avatarUrl,
          bankCode = profile.bankCode,
          accountNumber = profile.accountNumber,
          accountName = profile.accountName
        )
      )
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /** Lấy thông tin ngân hàng của một user khác để tạo QR code */
  suspend fun getUserProfile(userId: String): Result<ProfileResponse> {
    return try {
      val profile = postgrest["profiles"]
        .select { filter { eq("id", userId) } }
        .decodeSingle<ProfileEntity>()

      Result.success(
        ProfileResponse(
          id = profile.id,
          username = profile.username,
          email = profile.email,
          avatarUrl = profile.avatarUrl,
          bankCode = profile.bankCode,
          accountNumber = profile.accountNumber,
          accountName = profile.accountName
        )
      )
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
      val userId = auth.currentUserOrNull()?.id
        ?: return Result.failure(Exception("Chưa đăng nhập"))

      postgrest["profiles"].update({
        set("bank_code", bankCode)
        set("account_number", accountNumber)
        set("account_name", accountName)
      }) { filter { eq("id", userId) } }

      Result.success("Cập nhật thông tin ngân hàng thành công")
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /** Upload ảnh đại diện */
  suspend fun uploadAvatar(imageBytes: ByteArray): Result<String> {
    return try {
      val userId = auth.currentUserOrNull()?.id
        ?: return Result.failure(Exception("Chưa đăng nhập"))

      val fileName = "$userId/${UUID.randomUUID()}.jpg"
      val bucket = storage["avatars"]
      bucket.upload(fileName, imageBytes) {
        upsert = true
      }
      val publicUrl = bucket.publicUrl(fileName)

      postgrest["profiles"].update({
        set("avatar_url", publicUrl)
      }) { filter { eq("id", userId) } }

      Result.success(publicUrl)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
