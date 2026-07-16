package com.example.splitbill.data

import com.example.splitbill.data.api.ApiService
import com.example.splitbill.data.api.CreateInviteRequest
import com.example.splitbill.data.api.InviteResponse
import com.example.splitbill.data.api.JoinGroupWithCodeRequest
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.flow.first

class InviteRepository(private val tokenManager: TokenManager) {

  private suspend fun getClient() = ApiService.createClient(tokenManager.getToken().first())

  suspend fun createInvite(groupId: String, maxUses: Int?): Result<InviteResponse> {
    return try {
      val response: InviteResponse = getClient().post("/api/groups/$groupId/invites") {
        setBody(CreateInviteRequest(maxUses))
      }.body()
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getActiveInvites(groupId: String): Result<List<InviteResponse>> {
    return try {
      val response: List<InviteResponse> = getClient().get("/api/groups/$groupId/invites").body()
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun joinByInvite(inviteCode: String): Result<InviteResponse> {
    return try {
      val response: InviteResponse = getClient().post("/api/invites/join") {
        setBody(JoinGroupWithCodeRequest(inviteCode))
      }.body()
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteInvite(inviteId: String): Result<Boolean> {
    return try {
      getClient().delete("/api/invites/$inviteId")
      Result.success(true)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
