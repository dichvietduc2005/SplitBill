package com.example.splitbill.data

import com.example.splitbill.data.api.ApiService
import com.example.splitbill.data.api.GroupResponse
import com.example.splitbill.data.api.CreateGroupRequest
import com.example.splitbill.data.api.AddMemberRequest
import com.example.splitbill.data.api.MemberResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.flow.first

class GroupRepository(private val tokenManager: TokenManager) {

  private suspend fun getClient() = ApiService.createClient(tokenManager.getToken().first())

  suspend fun getGroups(): Result<List<GroupResponse>> {
    return try {
      val response: List<GroupResponse> = getClient().get("/api/groups").body()
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getGroupDetails(groupId: String): Result<GroupResponse> {
    return try {
      val response: GroupResponse = getClient().get("/api/groups/$groupId").body()
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun createGroup(name: String): Result<GroupResponse> {
    return try {
      val response: GroupResponse = getClient().post("/api/groups") {
        setBody(CreateGroupRequest(name))
      }.body()
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getMembers(groupId: String): Result<List<MemberResponse>> {
    return try {
      val response: List<MemberResponse> = getClient().get("/api/groups/$groupId/members").body()
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun addMember(groupId: String, usernameOrEmail: String): Result<String> {
    return try {
      val response: Map<String, String> = getClient().post("/api/groups/$groupId/members") {
        setBody(AddMemberRequest(usernameOrEmail))
      }.body()
      Result.success(response["message"] ?: "Đã thêm thành viên")
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun joinGroup(groupId: String): Result<String> {
    return try {
      val response: Map<String, String> = getClient().post("/api/groups/$groupId/join").body()
      Result.success(response["message"] ?: "Đã tham gia nhóm")
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
