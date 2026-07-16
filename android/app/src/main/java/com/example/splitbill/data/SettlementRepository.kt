package com.example.splitbill.data

import com.example.splitbill.data.api.ApiService
import com.example.splitbill.data.api.CreateSettlementRequest
import com.example.splitbill.data.api.SettlementResponse
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.flow.first

class SettlementRepository(private val tokenManager: TokenManager) {

  private suspend fun getClient() = ApiService.createClient(tokenManager.getToken().first())

  suspend fun createSettlement(
    groupId: String,
    toUserId: String,
    amount: Double,
    note: String?
  ): Result<SettlementResponse> {
    return try {
      val response: SettlementResponse = getClient().post("/api/settlements") {
        setBody(
          CreateSettlementRequest(
            groupId = groupId,
            toUserId = toUserId,
            amount = amount,
            note = note
          )
        )
      }.body()
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
