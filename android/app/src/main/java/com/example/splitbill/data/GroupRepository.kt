package com.example.splitbill.data

import com.example.splitbill.data.api.GroupResponse
import com.example.splitbill.data.api.MemberResponse
import com.example.splitbill.data.api.PaginatedActivityResponse
import com.example.splitbill.data.api.ActivityResponse
import com.example.splitbill.data.supabase.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

class GroupRepository(private val tokenManager: TokenManager) {

  private val postgrest get() = SupabaseConfig.client.postgrest
  private val auth get() = SupabaseConfig.client.auth

  suspend fun getGroups(): Result<List<GroupResponse>> {
    return try {
      val currentUserId = auth.currentUserOrNull()?.id 
        ?: TokenManager.getUserIdFromToken(tokenManager.getCachedToken())
      if (currentUserId == null) {
        return Result.failure(Exception("Chưa đăng nhập"))
      }

      val memberRecords = postgrest["group_members"]
        .select { filter { eq("user_id", currentUserId) } }
        .decodeList<GroupMemberEntity>()

      if (memberRecords.isEmpty()) {
        return Result.success(emptyList())
      }

      val groupIds = memberRecords.map { it.groupId }
      val groups = postgrest["groups"]
        .select { filter { isIn("id", groupIds) } }
        .decodeList<GroupEntity>()

      val allMembers = postgrest["group_members"]
        .select { filter { isIn("group_id", groupIds) } }
        .decodeList<GroupMemberEntity>()
      val memberCountMap = allMembers.groupBy { it.groupId }.mapValues { it.value.size }

      val result = groups.map { g ->
        GroupResponse(
          id = g.id,
          name = g.name,
          createdBy = g.createdBy ?: "",
          createdByName = "",
          memberCount = memberCountMap[g.id] ?: 1,
          createdAt = g.createdAt ?: ""
        )
      }
      Result.success(result)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getGroupDetails(groupId: String): Result<GroupResponse> {
    return try {
      val group = postgrest["groups"]
        .select { filter { eq("id", groupId) } }
        .decodeSingle<GroupEntity>()

      val members = postgrest["group_members"]
        .select { filter { eq("group_id", groupId) } }
        .decodeList<GroupMemberEntity>()

      Result.success(
        GroupResponse(
          id = group.id,
          name = group.name,
          createdBy = group.createdBy ?: "",
          createdByName = "",
          memberCount = members.size,
          createdAt = group.createdAt ?: ""
        )
      )
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun createGroup(name: String): Result<GroupResponse> {
    return try {
      val currentUserId = auth.currentUserOrNull()?.id
        ?: TokenManager.getUserIdFromToken(tokenManager.getCachedToken())
        ?: return Result.failure(Exception("Chưa đăng nhập"))

      android.util.Log.d("SplitBill_Group", "Bắt đầu tạo nhóm '$name' bởi user: $currentUserId")

      val newGroupId = java.util.UUID.randomUUID().toString()

      // Dùng GroupEntity thay mapOf để đảm bảo serialize đúng kiểu UUID
      postgrest["groups"].insert(
        GroupEntity(
          id = newGroupId,
          name = name,
          createdBy = currentUserId,
          createdAt = java.time.Instant.now().toString()
        )
      )

      postgrest["group_members"].insert(
        GroupMemberEntity(groupId = newGroupId, userId = currentUserId, role = "OWNER")
      )

      android.util.Log.d("SplitBill_Group", "Tạo nhóm '$name' thành công với id: $newGroupId")

      Result.success(
        GroupResponse(
          id = newGroupId,
          name = name,
          createdBy = currentUserId,
          createdByName = "",
          memberCount = 1,
          createdAt = ""
        )
      )
    } catch (e: Exception) {
      android.util.Log.e("SplitBill_Group", "Lỗi tạo nhóm: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun getMembers(groupId: String): Result<List<MemberResponse>> {
    return try {
      val memberRecords = postgrest["group_members"]
        .select { filter { eq("group_id", groupId) } }
        .decodeList<GroupMemberEntity>()

      val userIds = memberRecords.map { it.userId }
      val profiles = if (userIds.isNotEmpty()) {
        postgrest["profiles"]
          .select { filter { isIn("id", userIds) } }
          .decodeList<ProfileEntity>()
      } else emptyList()
      val profileMap = profiles.associateBy { it.id }

      val result = memberRecords.map { m ->
        val p = profileMap[m.userId]
        MemberResponse(
          userId = m.userId,
          username = p?.username ?: "User",
          email = p?.email ?: "",
          avatarUrl = p?.avatarUrl,
          joinedAt = m.joinedAt ?: ""
        )
      }
      Result.success(result)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun addMember(groupId: String, usernameOrEmail: String): Result<String> {
    return try {
      val cleanQuery = usernameOrEmail.trim()
      val profiles = postgrest["profiles"]
        .select()
        .decodeList<ProfileEntity>()
      
      val profile = profiles.firstOrNull { 
        it.username.equals(cleanQuery, ignoreCase = true) || it.email.equals(cleanQuery, ignoreCase = true) 
      } ?: return Result.failure(Exception("Không tìm thấy người dùng: $cleanQuery"))

      postgrest["group_members"].insert(
        GroupMemberEntity(groupId = groupId, userId = profile.id, role = "MEMBER")
      )
      Result.success("Đã thêm thành viên ${profile.username}")
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun joinGroup(groupId: String): Result<String> {
    return try {
      val currentUserId = auth.currentUserOrNull()?.id
        ?: TokenManager.getUserIdFromToken(tokenManager.getCachedToken())
        ?: return Result.failure(Exception("Chưa đăng nhập"))

      postgrest["group_members"].insert(
        GroupMemberEntity(groupId = groupId, userId = currentUserId, role = "MEMBER")
      )
      Result.success("Đã tham gia nhóm thành công")
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getActivities(groupId: String, limit: Int = 50, offset: Int = 0): Result<PaginatedActivityResponse> {
    return try {
      val logs = postgrest["activity_logs"]
        .select {
          filter { eq("group_id", groupId) }
        }
        .decodeList<ActivityLogEntity>()

      val items: List<ActivityResponse> = logs.map { log ->
        ActivityResponse(
          id = log.id ?: "",
          groupId = log.groupId,
          userId = log.userId,
          username = "Thành viên",
          activityType = log.activityType,
          description = log.description,
          createdAt = log.createdAt ?: ""
        )
      }
      Result.success(
        PaginatedActivityResponse(
          data = items,
          total = items.size.toLong(),
          limit = limit,
          offset = offset
        )
      )
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
