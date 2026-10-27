package com.example.splitbill.data

import com.example.splitbill.data.api.GroupResponse
import com.example.splitbill.data.api.MemberResponse
import com.example.splitbill.data.api.PaginatedActivityResponse
import com.example.splitbill.data.api.ActivityResponse
import com.example.splitbill.data.supabase.*
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GroupRepository(
  private val tokenManager: TokenManager,
  private val groupDao: com.example.splitbill.data.local.dao.GroupDao? = null
) {

  private val postgrest get() = SupabaseConfig.client.postgrest
  private val auth get() = SupabaseConfig.client.auth
  private val storage get() = SupabaseConfig.client.storage

  /**
   * Quan sát danh sách nhóm từ Room Database theo mô hình Single Source of Truth (SSOT).
   */
  fun observeGroups(): Flow<List<GroupResponse>>? {
    val dao = groupDao ?: return null
    return dao.observeGroups().map { localGroups ->
      localGroups.map { g ->
        GroupResponse(
          id = g.id,
          name = g.name,
          createdBy = g.createdBy,
          createdByName = g.createdByName,
          memberCount = g.memberCount,
          createdAt = g.createdAt ?: "",
          bannerUrl = getLocalGroupBanner(g.id)
        )
      }
    }
  }

  fun sanitizeBannerUrl(url: String?): String? {
    if (url.isNullOrBlank() || url == "default") return null
    if (url.startsWith("file:", ignoreCase = true) || url.startsWith("/")) {
      return try {
        val uri = android.net.Uri.parse(url)
        val path = uri.path ?: url
        val file = java.io.File(path)
        if (file.exists()) url else null
      } catch (_: Exception) { null }
    }
    return url
  }

  fun getLocalGroupBanner(groupId: String): String? {
    return try {
      val path = tokenManager.getContext().getSharedPreferences("group_banners", android.content.Context.MODE_PRIVATE)
        .getString("banner_$groupId", null)
      sanitizeBannerUrl(path)
    } catch (_: Exception) { null }
  }

  val bannerUpdateFlow = kotlinx.coroutines.flow.MutableSharedFlow<Pair<String, String>>(
    replay = 1,
    extraBufferCapacity = 10,
    onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
  )

  fun saveLocalGroupBanner(groupId: String, bannerUrl: String) {
    try {
      tokenManager.getContext().getSharedPreferences("group_banners", android.content.Context.MODE_PRIVATE)
        .edit()
        .putString("banner_$groupId", bannerUrl)
        .apply()
      bannerUpdateFlow.tryEmit(Pair(groupId, bannerUrl))
    } catch (_: Exception) {}
  }

  suspend fun updateGroupBanner(groupId: String, bannerUrl: String): Result<Unit> {
    saveLocalGroupBanner(groupId, bannerUrl)
    groupDao?.let { dao ->
      try {
        val local = dao.getGroupById(groupId)
        if (local != null) {
          dao.insertGroup(local.copy(updatedAt = System.currentTimeMillis()))
        }
      } catch (_: Exception) {}
    }
    // Chỉ cập nhật lên Supabase nếu là URL đám mây https:// hoặc http://
    if (bannerUrl.startsWith("http://", ignoreCase = true) || bannerUrl.startsWith("https://", ignoreCase = true)) {
      return try {
        SupabaseConfig.ensureSession(tokenManager.getCachedToken(), tokenManager)
        postgrest["groups"].update({
          set("icon", bannerUrl)
        }) {
          filter { eq("id", groupId) }
        }
        Result.success(Unit)
      } catch (e: Exception) {
        Result.success(Unit)
      }
    }
    return Result.success(Unit)
  }

  suspend fun uploadGroupBanner(groupId: String, imageBytes: ByteArray): Result<String> {
    return try {
      val cachedToken = tokenManager.getCachedToken()
      SupabaseConfig.ensureSession(cachedToken, tokenManager)
      val fileName = "banners/$groupId/${java.util.UUID.randomUUID()}.jpg"

      // Tải lên Supabase Storage (thử bucket avatars hoặc receipts)
      val publicUrl = try {
        val bucket = storage["avatars"]
        bucket.upload(fileName, imageBytes) { upsert = true }
        bucket.publicUrl(fileName)
      } catch (_: Exception) {
        val bucket = storage["receipts"]
        bucket.upload(fileName, imageBytes) { upsert = true }
        bucket.publicUrl(fileName)
      }

      updateGroupBanner(groupId, publicUrl)
      Result.success(publicUrl)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getGroups(): Result<List<GroupResponse>> {
    return try {
      val cachedToken = tokenManager.getCachedToken()
      val sessionValid = SupabaseConfig.ensureSession(cachedToken, tokenManager)
      val currentUserId = auth.currentUserOrNull()?.id ?: tokenManager.getUserId()
      if (!sessionValid || currentUserId == null || auth.currentSessionOrNull() == null) {
        tokenManager.deleteToken()
        throw Exception("Chưa đăng nhập hoặc phiên đăng nhập đã hết hạn")
      }

      // Tải song song danh sách nhóm trực tiếp và thành viên nhóm
      var dgFailed = false
      var mrFailed = false
      val (directGroups, memberRecords) = coroutineScope {
        val dgDeferred = async {
          try {
            postgrest["groups"].select().decodeList<GroupEntity>()
          } catch (e: Exception) {
            if (SupabaseConfig.isAuthError(e)) throw e
            android.util.Log.w("GroupRepository", "Query direct groups failed: ${e.message}")
            dgFailed = true
            emptyList()
          }
        }
        val mrDeferred = async {
          try {
            postgrest["group_members"]
              .select { filter { eq("user_id", currentUserId) } }
              .decodeList<GroupMemberEntity>()
          } catch (e: Exception) {
            if (SupabaseConfig.isAuthError(e)) throw e
            android.util.Log.w("GroupRepository", "Query group_members failed: ${e.message}")
            mrFailed = true
            emptyList()
          }
        }
        Pair(dgDeferred.await(), mrDeferred.await())
      }

      if (dgFailed && mrFailed) {
        val localGroups = groupDao?.getGroups() ?: emptyList()
        if (localGroups.isNotEmpty()) {
          return Result.success(localGroups.map { g ->
            GroupResponse(
              id = g.id,
              name = g.name,
              createdBy = g.createdBy,
              createdByName = g.createdByName,
              memberCount = g.memberCount,
              createdAt = g.createdAt ?: "",
              bannerUrl = getLocalGroupBanner(g.id)
            )
          })
        }
        throw Exception("Không thể kết nối đến máy chủ để tải danh sách nhóm")
      }

      val memberGroupIds = memberRecords.map { it.groupId }
      val missingIds = memberGroupIds - directGroups.map { it.id }.toSet()
      val groupsFromMemberIds = if (missingIds.isNotEmpty()) {
        try {
          postgrest["groups"]
            .select { filter { isIn("id", missingIds) } }
            .decodeList<GroupEntity>()
        } catch (e: Exception) {
          if (SupabaseConfig.isAuthError(e)) throw e
          emptyList()
        }
      } else emptyList()

      // Hợp nhất danh sách nhóm và sắp xếp thời gian tạo giảm dần cố định (tránh nhảy vị trí khi load)
      val groups = (directGroups + groupsFromMemberIds)
        .distinctBy { it.id }
        .sortedByDescending { it.createdAt ?: "" }

      if (groups.isEmpty()) {
        val localGroups = groupDao?.getGroups() ?: emptyList()
        if (localGroups.isNotEmpty() && (dgFailed || mrFailed)) {
          return Result.success(localGroups.map { g ->
            GroupResponse(
              id = g.id,
              name = g.name,
              createdBy = g.createdBy,
              createdByName = g.createdByName,
              memberCount = g.memberCount,
              createdAt = g.createdAt ?: "",
              bannerUrl = getLocalGroupBanner(g.id)
            )
          })
        }
        return Result.success(emptyList())
      }

      val groupIds = groups.map { it.id }
      val allMembers = try {
        postgrest["group_members"]
          .select { filter { isIn("group_id", groupIds) } }
          .decodeList<GroupMemberEntity>()
      } catch (e: Exception) {
        if (SupabaseConfig.isAuthError(e)) throw e
        emptyList()
      }
      val memberCountMap = allMembers.groupBy { it.groupId }.mapValues { it.value.size }

      val result = groups.map { g ->
        val banner = getLocalGroupBanner(g.id) ?: sanitizeBannerUrl(g.icon)
        if (banner != null && getLocalGroupBanner(g.id) == null) {
          saveLocalGroupBanner(g.id, banner)
        }
        GroupResponse(
          id = g.id,
          name = g.name,
          createdBy = g.createdBy ?: "",
          createdByName = "",
          memberCount = memberCountMap[g.id] ?: 1,
          createdAt = g.createdAt ?: "",
          bannerUrl = banner
        )
      }

      // Lưu cache vào Room
      groupDao?.let { dao ->
        val localEntities = groups.map { g ->
          com.example.splitbill.data.local.entity.GroupEntityLocal(
            id = g.id,
            name = g.name,
            description = g.description,
            createdBy = g.createdBy ?: "",
            createdByName = "",
            memberCount = memberCountMap[g.id] ?: 1,
            createdAt = g.createdAt
          )
        }
        dao.insertGroups(localEntities)
      }

      Result.success(result)
    } catch (e: Exception) {
      if (SupabaseConfig.isAuthError(e)) {
        tokenManager.deleteToken()
      }
      // Fallback offline từ Room cache
      if (groupDao != null) {
        try {
          val localGroups = groupDao.getGroups()
          if (localGroups.isNotEmpty()) {
            val cachedResult = localGroups.map { g ->
              GroupResponse(
                id = g.id,
                name = g.name,
                createdBy = g.createdBy,
                createdByName = g.createdByName,
                memberCount = g.memberCount,
                createdAt = g.createdAt ?: "",
                bannerUrl = getLocalGroupBanner(g.id)
              )
            }
            return Result.success(cachedResult)
          }
        } catch (_: Exception) {}
      }
      Result.failure(e)
    }
  }


  suspend fun getGroupDetails(groupId: String): Result<GroupResponse> {
    return try {
      SupabaseConfig.ensureSession(tokenManager.getCachedToken(), tokenManager)
      if (auth.currentSessionOrNull() == null) {
        return Result.failure(Exception("Chưa đăng nhập hoặc phiên đăng nhập đã hết hạn"))
      }
      val group = postgrest["groups"]
        .select { filter { eq("id", groupId) } }
        .decodeSingle<GroupEntity>()

      val members = postgrest["group_members"]
        .select { filter { eq("group_id", groupId) } }
        .decodeList<GroupMemberEntity>()

      val banner = getLocalGroupBanner(group.id) ?: sanitizeBannerUrl(group.icon)
      if (banner != null && getLocalGroupBanner(group.id) == null) {
        saveLocalGroupBanner(group.id, banner)
      }

      Result.success(
        GroupResponse(
          id = group.id,
          name = group.name,
          createdBy = group.createdBy ?: "",
          createdByName = "",
          memberCount = members.size,
          createdAt = group.createdAt ?: "",
          bannerUrl = banner
        )
      )
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun createGroup(name: String): Result<GroupResponse> {
    return try {
      SupabaseConfig.ensureSession(tokenManager.getCachedToken(), tokenManager)
      val currentUserId = auth.currentUserOrNull()?.id
        ?: return Result.failure(Exception("Chưa đăng nhập hoặc phiên đăng nhập đã hết hạn"))

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
