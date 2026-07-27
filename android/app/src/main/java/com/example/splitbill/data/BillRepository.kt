package com.example.splitbill.data

import com.example.splitbill.data.api.ApiService
import com.example.splitbill.data.api.BillResponse
import com.example.splitbill.data.api.CreateBillRequest
import com.example.splitbill.data.api.BillSplitItem
import com.example.splitbill.data.api.DebtResponse
import com.example.splitbill.data.api.UpdateBillRequest
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import kotlinx.coroutines.flow.first
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

class BillRepository(private val tokenManager: TokenManager) {

  private suspend fun getClient() = ApiService.createClient(tokenManager.getToken().first())

  suspend fun getBillsForGroup(groupId: String): Result<List<BillResponse>> {
    return try {
      val response: com.example.splitbill.data.api.PaginatedBillResponse = getClient().get("/api/bills?groupId=$groupId").body()
      Result.success(response.data)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun createBill(
    groupId: String,
    description: String,
    totalAmount: Double,
    paidByUserId: String,
    currency: String,
    exchangeRate: Double,
    splits: List<BillSplitItem>
  ): Result<BillResponse> {
    return try {
      val response: BillResponse = getClient().post("/api/bills") {
        setBody(
          CreateBillRequest(
            groupId = groupId,
            description = description,
            totalAmount = totalAmount,
            paidByUserId = paidByUserId,
            currency = currency,
            exchangeRate = exchangeRate,
            splits = splits
          )
        )
      }.body()
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updateBill(
    billId: String,
    description: String,
    totalAmount: Double,
    paidByUserId: String,
    currency: String,
    exchangeRate: Double,
    splits: List<BillSplitItem>
  ): Result<BillResponse> {
    return try {
      val response: BillResponse = getClient().put("/api/bills/$billId") {
        setBody(
          UpdateBillRequest(
            description = description,
            totalAmount = totalAmount,
            paidByUserId = paidByUserId,
            currency = currency,
            exchangeRate = exchangeRate,
            splits = splits
          )
        )
      }.body()
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteBill(billId: String): Result<Unit> {
    return try {
      getClient().delete("/api/bills/$billId")
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun uploadReceipt(billId: String, imageBytes: ByteArray): Result<String> {
    return try {
      val response: Map<String, String> = getClient().post("/api/bills/$billId/receipt") {
        setBody(
          MultiPartFormDataContent(
            formData {
              append("receipt", imageBytes, Headers.build {
                append(HttpHeaders.ContentType, "image/jpeg")
                append(HttpHeaders.ContentDisposition, "filename=\"receipt.jpg\"")
              })
            }
          )
        )
      }.body()
      Result.success(response["receiptUrl"] ?: "")
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getDebtsForGroup(groupId: String): Result<DebtResponse> {
    return try {
      val response: DebtResponse = getClient().get("/api/debts/$groupId").body()
      Result.success(response)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun markBillAsPaid(billId: String, isPaid: Boolean): Result<Unit> {
    return try {
      getClient().post("/api/bills/$billId/pay?isPaid=$isPaid")
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
