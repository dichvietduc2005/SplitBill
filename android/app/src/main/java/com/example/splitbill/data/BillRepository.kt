package com.example.splitbill.data

import com.example.splitbill.data.api.BillResponse
import com.example.splitbill.data.api.BillSplitItem
import com.example.splitbill.data.api.BillSplitResponse
import com.example.splitbill.data.api.DebtResponse
import com.example.splitbill.data.api.PaginatedBillResponse
import com.example.splitbill.data.supabase.*
import com.example.splitbill.utils.DebtSimplifier
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import java.util.UUID

class BillRepository(private val tokenManager: TokenManager) {

  private val postgrest get() = SupabaseConfig.client.postgrest
  private val storage get() = SupabaseConfig.client.storage
  private val auth get() = SupabaseConfig.client.auth

  suspend fun getBillsForGroup(groupId: String): Result<List<BillResponse>> {
    return try {
      val bills = postgrest["bills"]
        .select { filter { eq("group_id", groupId) } }
        .decodeList<BillEntity>()

      if (bills.isEmpty()) {
        return Result.success(emptyList())
      }

      val billIds = bills.mapNotNull { it.id }
      val splits = postgrest["bill_splits"]
        .select { filter { isIn("bill_id", billIds) } }
        .decodeList<BillSplitEntity>()

      val profiles = postgrest["profiles"]
        .select()
        .decodeList<ProfileEntity>()
      val profileMap = profiles.associateBy { it.id }

      val splitsByBillId = splits.groupBy { it.billId }

      val response = bills.map { b ->
        val billSplits = splitsByBillId[b.id] ?: emptyList()
        val splitResponses = billSplits.map { s ->
          BillSplitResponse(
            userId = s.userId,
            username = profileMap[s.userId]?.username ?: "User",
            amountOwed = s.amountOwed
          )
        }

        BillResponse(
          id = b.id ?: "",
          groupId = b.groupId,
          description = b.description,
          totalAmount = b.totalAmount,
          paidByUserId = b.paidByUserId,
          paidByUsername = profileMap[b.paidByUserId]?.username ?: "User",
          currency = b.currency,
          exchangeRate = b.exchangeRate,
          category = b.category,
          receiptUrl = b.receiptUrl,
          isPaid = b.isPaid,
          splits = splitResponses,
          createdAt = b.createdAt ?: ""
        )
      }
      Result.success(response)
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
    splits: List<BillSplitItem>,
    category: String = "GENERAL"
  ): Result<BillResponse> {
    return try {
      val billId = UUID.randomUUID().toString()

      postgrest["bills"].insert(
        BillEntity(
          id = billId,
          groupId = groupId,
          description = description,
          totalAmount = totalAmount,
          paidByUserId = paidByUserId,
          currency = currency,
          exchangeRate = exchangeRate,
          category = category
        )
      )

      val splitEntities = splits.map {
        BillSplitEntity(
          billId = billId,
          userId = it.userId,
          amountOwed = it.amount
        )
      }

      if (splitEntities.isNotEmpty()) {
        postgrest["bill_splits"].insert(splitEntities)
      }

      val profiles = postgrest["profiles"]
        .select()
        .decodeList<ProfileEntity>()
      val profileMap = profiles.associateBy { it.id }

      val splitResponses = splits.map {
        BillSplitResponse(
          userId = it.userId,
          username = profileMap[it.userId]?.username ?: "User",
          amountOwed = it.amount
        )
      }

      // Log activity
      try {
        val currentUserId = auth.currentUserOrNull()?.id ?: paidByUserId
        val actorName = profileMap[currentUserId]?.username ?: "Ai đó"
        postgrest["activity_logs"].insert(
          ActivityLogEntity(
            groupId = groupId,
            userId = currentUserId,
            activityType = "BILL_CREATED",
            description = "$actorName đã thêm hóa đơn '$description'"
          )
        )
      } catch (_: Exception) {}

      Result.success(
        BillResponse(
          id = billId,
          groupId = groupId,
          description = description,
          totalAmount = totalAmount,
          paidByUserId = paidByUserId,
          paidByUsername = profileMap[paidByUserId]?.username ?: "User",
          currency = currency,
          exchangeRate = exchangeRate,
          category = category,
          receiptUrl = null,
          isPaid = false,
          splits = splitResponses,
          createdAt = java.time.Instant.now().toString()
        )
      )
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
    splits: List<BillSplitItem>,
    category: String = "GENERAL"
  ): Result<BillResponse> {
    return try {
      postgrest["bills"].update({
        set("description", description)
        set("total_amount", totalAmount)
        set("paid_by_user_id", paidByUserId)
        set("currency", currency)
        set("exchange_rate", exchangeRate)
        set("category", category)
      }) { filter { eq("id", billId) } }

      // Cập nhật lại splits
      postgrest["bill_splits"].delete { filter { eq("bill_id", billId) } }
      val splitEntities = splits.map {
        BillSplitEntity(
          billId = billId,
          userId = it.userId,
          amountOwed = it.amount
        )
      }
      if (splitEntities.isNotEmpty()) {
        postgrest["bill_splits"].insert(splitEntities)
      }

      val profiles = postgrest["profiles"]
        .select()
        .decodeList<ProfileEntity>()
      val profileMap = profiles.associateBy { it.id }

      val splitResponses = splits.map {
        BillSplitResponse(
          userId = it.userId,
          username = profileMap[it.userId]?.username ?: "User",
          amountOwed = it.amount
        )
      }

      val updatedBill = postgrest["bills"]
        .select { filter { eq("id", billId) } }
        .decodeSingle<BillEntity>()

      Result.success(
        BillResponse(
          id = billId,
          groupId = updatedBill.groupId,
          description = description,
          totalAmount = totalAmount,
          paidByUserId = paidByUserId,
          paidByUsername = profileMap[paidByUserId]?.username ?: "User",
          currency = currency,
          exchangeRate = exchangeRate,
          category = category,
          receiptUrl = updatedBill.receiptUrl,
          isPaid = updatedBill.isPaid,
          splits = splitResponses,
          createdAt = updatedBill.createdAt ?: ""
        )
      )
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteBill(billId: String): Result<Unit> {
    return try {
      postgrest["bills"].delete { filter { eq("id", billId) } }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun uploadReceipt(billId: String, imageBytes: ByteArray): Result<String> {
    return try {
      val fileName = "$billId/${UUID.randomUUID()}.jpg"
      val bucket = storage["receipts"]
      bucket.upload(fileName, imageBytes) {
        upsert = true
      }
      val publicUrl = bucket.publicUrl(fileName)

      postgrest["bills"].update({
        set("receipt_url", publicUrl)
      }) { filter { eq("id", billId) } }

      Result.success(publicUrl)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getSimplifiedDebts(groupId: String): Result<DebtResponse> {
    return try {
      val billsResult = getBillsForGroup(groupId)
      val bills = billsResult.getOrNull() ?: emptyList()

      val settlements = postgrest["settlements"]
        .select { filter { eq("group_id", groupId) } }
        .decodeList<SettlementEntity>()

      val profiles = postgrest["profiles"]
        .select()
        .decodeList<ProfileEntity>()
      val profileMap = profiles.associate { it.id to it.username }

      val settlementResponses = settlements.map { s ->
        com.example.splitbill.data.api.SettlementResponse(
          id = s.id ?: "",
          groupId = s.groupId,
          fromUserId = s.fromUserId,
          fromUsername = profileMap[s.fromUserId] ?: "User",
          toUserId = s.toUserId,
          toUsername = profileMap[s.toUserId] ?: "User",
          amount = s.amount,
          note = s.note,
          createdAt = s.createdAt ?: ""
        )
      }

      val group = postgrest["groups"]
        .select { filter { eq("id", groupId) } }
        .decodeSingle<GroupEntity>()

      val simplified = DebtSimplifier.simplify(bills, settlementResponses, profileMap)

      Result.success(
        DebtResponse(
          groupId = groupId,
          groupName = group.name,
          debts = simplified,
          totalTransactions = simplified.size
        )
      )
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updateBillPaidStatus(billId: String, isPaid: Boolean): Result<Boolean> {
    return try {
      postgrest["bills"].update({
        set("is_paid", isPaid)
      }) { filter { eq("id", billId) } }
      Result.success(true)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getDebtsForGroup(groupId: String): Result<DebtResponse> = getSimplifiedDebts(groupId)

  suspend fun markBillAsPaid(billId: String, isPaid: Boolean): Result<Boolean> = updateBillPaidStatus(billId, isPaid)
}
