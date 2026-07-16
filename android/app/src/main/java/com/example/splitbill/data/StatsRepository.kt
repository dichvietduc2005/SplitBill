package com.example.splitbill.data

import com.example.splitbill.data.api.ApiService
import com.example.splitbill.data.api.UserStatsResponse
import com.example.splitbill.data.api.GroupStatsResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.first

class StatsRepository(private val tokenManager: TokenManager) {

  private suspend fun getClient() = ApiService.createClient(tokenManager.getToken().first())

  suspend fun getUserStats(): Result<UserStatsResponse> {
    return try {
      val response: UserStatsResponse = getClient().get("/api/stats/me").body()
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getGroupStats(groupId: String): Result<GroupStatsResponse> {
    return try {
      val response: GroupStatsResponse = getClient().get("/api/stats/group/$groupId").body()
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
