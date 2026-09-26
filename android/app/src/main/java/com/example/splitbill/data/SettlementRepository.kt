package com.example.splitbill.data

import com.example.splitbill.data.api.SettlementResponse
import com.example.splitbill.data.supabase.ActivityLogEntity
import com.example.splitbill.data.supabase.ProfileEntity
import com.example.splitbill.data.supabase.SettlementEntity
import com.example.splitbill.data.supabase.SupabaseConfig
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

class SettlementRepository(private val tokenManager: TokenManager) {

  private val postgrest get() = SupabaseConfig.client.postgrest
  private val auth get() = SupabaseConfig.client.auth

  suspend fun createSettlement(
    groupId: String,
    toUserId: String,
    amount: Double,
    note: String?,
    fromUserId: String? = null
  ): Result<SettlementResponse> {
    return try {
      val actualFromUserId = fromUserId ?: auth.currentUserOrNull()?.id
        ?: TokenManager.getUserIdFromToken(tokenManager.getCachedToken())
        ?: return Result.failure(Exception("Chưa đăng nhập"))

      val entity = postgrest["settlements"].insert(
        SettlementEntity(
          groupId = groupId,
          fromUserId = actualFromUserId,
          toUserId = toUserId,
          amount = amount,
          note = note
        )
      ) { select() }.decodeSingle<SettlementEntity>()

      val profiles = postgrest["profiles"]
        .select { filter { isIn("id", listOf(actualFromUserId, toUserId)) } }
        .decodeList<ProfileEntity>()
      val profileMap = profiles.associateBy { it.id }

      val fromName = profileMap[actualFromUserId]?.username ?: "User"
      val toName = profileMap[toUserId]?.username ?: "User"

      // Ghi log hoạt động
      try {
        postgrest["activity_logs"].insert(
          ActivityLogEntity(
            groupId = groupId,
            userId = actualFromUserId,
            activityType = "SETTLEMENT_CREATED",
            description = "$fromName đã trả cho $toName ${amount.toLong()} VND"
          )
        )
      } catch (_: Exception) {}

      Result.success(
        SettlementResponse(
          id = entity.id ?: "",
          groupId = groupId,
          fromUserId = actualFromUserId,
          fromUsername = fromName,
          toUserId = toUserId,
          toUsername = toName,
          amount = amount,
          note = note,
          createdAt = entity.createdAt ?: ""
        )
      )
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
