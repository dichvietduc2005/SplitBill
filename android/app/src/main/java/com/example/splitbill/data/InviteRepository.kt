package com.example.splitbill.data

import com.example.splitbill.data.api.InviteResponse
import com.example.splitbill.data.supabase.GroupEntity
import com.example.splitbill.data.supabase.GroupMemberEntity
import com.example.splitbill.data.supabase.SupabaseConfig
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

class InviteRepository(private val tokenManager: TokenManager) {

  private val postgrest get() = SupabaseConfig.client.postgrest
  private val auth get() = SupabaseConfig.client.auth

  suspend fun createInvite(groupId: String, maxUses: Int?): Result<InviteResponse> {
    return try {
      val group = postgrest["groups"]
        .select { filter { eq("id", groupId) } }
        .decodeSingle<GroupEntity>()

      val shortCode = if (groupId.length >= 8) groupId.substring(0, 8) else groupId
      Result.success(
        InviteResponse(
          id = groupId,
          groupId = groupId,
          groupName = group.name,
          inviteCode = shortCode,
          inviteUrl = "splitbill://invite/$groupId",
          expiresAt = "",
          maxUses = maxUses,
          useCount = 0,
          createdAt = ""
        )
      )
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getActiveInvites(groupId: String): Result<List<InviteResponse>> {
    return try {
      val group = postgrest["groups"]
        .select { filter { eq("id", groupId) } }
        .decodeSingle<GroupEntity>()

      val shortCode = if (groupId.length >= 8) groupId.substring(0, 8) else groupId
      Result.success(
        listOf(
          InviteResponse(
            id = groupId,
            groupId = groupId,
            groupName = group.name,
            inviteCode = shortCode,
            inviteUrl = "splitbill://invite/$groupId",
            expiresAt = "",
            maxUses = null,
            useCount = 0,
            createdAt = ""
          )
        )
      )
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun joinByInvite(inviteCode: String): Result<InviteResponse> {
    return try {
      val userId = auth.currentUserOrNull()?.id
        ?: TokenManager.getUserIdFromToken(tokenManager.getCachedToken())
        ?: return Result.failure(Exception("Chưa đăng nhập"))

      val groups = postgrest["groups"]
        .select()
        .decodeList<GroupEntity>()

      val cleanCode = inviteCode.trim()
      val matchedGroup = groups.firstOrNull { 
        it.id.startsWith(cleanCode, ignoreCase = true) || it.id.equals(cleanCode, ignoreCase = true) 
      } ?: return Result.failure(Exception("Mã mời không hợp lệ"))

      postgrest["group_members"].upsert(
        GroupMemberEntity(groupId = matchedGroup.id, userId = userId, role = "MEMBER")
      )

      Result.success(
        InviteResponse(
          id = matchedGroup.id,
          groupId = matchedGroup.id,
          groupName = matchedGroup.name,
          inviteCode = cleanCode,
          inviteUrl = "splitbill://invite/${matchedGroup.id}",
          expiresAt = "",
          maxUses = null,
          useCount = 0,
          createdAt = ""
        )
      )
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteInvite(inviteId: String): Result<Boolean> {
    return Result.success(true)
  }
}
