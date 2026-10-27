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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class BillRepository(
  private val tokenManager: TokenManager,
  private val billDao: com.example.splitbill.data.local.dao.BillDao? = null,
  private val pendingSyncDao: com.example.splitbill.data.local.dao.PendingSyncDao? = null
) {

  private val postgrest get() = SupabaseConfig.client.postgrest
  private val storage get() = SupabaseConfig.client.storage
  private val auth get() = SupabaseConfig.client.auth

  /**
   * Quan sát hóa đơn theo mô hình SSOT (Single Source of Truth) từ Room Flow.
   */
  fun observeBillsForGroup(groupId: String): kotlinx.coroutines.flow.Flow<List<BillResponse>>? {
    val dao = billDao ?: return null
    return dao.observeBillsForGroup(groupId).map { localList ->
      val billIds = localList.map { it.id }
      val cachedSplits = dao.getSplitsForBills(billIds).groupBy { it.billId }
      localList.map { b ->
        val splits = cachedSplits[b.id]?.map { s ->
          BillSplitResponse(
            userId = s.userId,
            username = s.username,
            amountOwed = s.amountOwed
          )
        } ?: emptyList()
        BillResponse(
          id = b.id,
          groupId = b.groupId,
          description = b.title,
          totalAmount = b.totalAmount,
          paidByUserId = b.paidBy,
          paidByUsername = b.paidByUsername,
          currency = b.currency,
          exchangeRate = b.exchangeRate,
          category = b.category ?: "OTHER",
          receiptUrl = b.receiptImageUrl,
          isPaid = b.isPaid,
          splits = splits,
          createdAt = b.createdAt ?: ""
        )
      }
    }
  }

  suspend fun getBillsForGroup(groupId: String): Result<List<BillResponse>> {
    return try {
      val cachedToken = tokenManager.getCachedToken()
      SupabaseConfig.ensureSession(cachedToken, tokenManager)
      val currentUserId = auth.currentUserOrNull()?.id
      if (currentUserId == null || auth.currentSessionOrNull() == null) {
        throw Exception("Chưa đăng nhập hoặc phiên đăng nhập đã hết hạn")
      }

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

      val involvedUserIds = (bills.map { it.paidByUserId } + splits.map { it.userId }).filter { it.isNotBlank() }.distinct()
      val profiles = if (involvedUserIds.isNotEmpty()) {
        try {
          postgrest["profiles"]
            .select { filter { isIn("id", involvedUserIds) } }
            .decodeList<ProfileEntity>()
        } catch (_: Exception) {
          emptyList()
        }
      } else emptyList()
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

      // Lưu cache vào Room chính xác, không dùng giá trị hardcode
      billDao?.let { dao ->
        val localBills = bills.map { b ->
          com.example.splitbill.data.local.entity.BillEntityLocal(
            id = b.id ?: "",
            groupId = b.groupId,
            paidBy = b.paidByUserId,
            paidByUsername = profileMap[b.paidByUserId]?.username ?: "Thành viên",
            title = b.description,
            description = b.description,
            totalAmount = b.totalAmount,
            currency = b.currency,
            exchangeRate = b.exchangeRate,
            category = b.category,
            receiptImageUrl = b.receiptUrl,
            isPaid = b.isPaid,
            splitType = "EQUAL",
            createdAt = b.createdAt,
            isPendingSync = false
          )
        }
        val localSplits = splits.map { s ->
          com.example.splitbill.data.local.entity.BillSplitEntityLocal(
            billId = s.billId,
            userId = s.userId,
            username = profileMap[s.userId]?.username ?: "Thành viên",
            amountOwed = s.amountOwed,
            isSettled = s.isSettled
          )
        }
        dao.syncBillsWithSplits(groupId, localBills, localSplits)
      }

      Result.success(response)
    } catch (e: Exception) {
      if (SupabaseConfig.isAuthError(e)) {
        tokenManager.deleteToken()
      }
      // Fallback offline từ Room cache với đầy đủ thông tin chuẩn xác
      if (billDao != null) {
        try {
          val cachedBills = billDao.getBillsForGroup(groupId)
          if (cachedBills.isNotEmpty()) {
            val billIds = cachedBills.map { it.id }
            val cachedSplits = billDao.getSplitsForBills(billIds).groupBy { it.billId }
            val cachedResponse = cachedBills.map { b ->
              val splits = cachedSplits[b.id]?.map { s ->
                BillSplitResponse(
                  userId = s.userId,
                  username = s.username,
                  amountOwed = s.amountOwed
                )
              } ?: emptyList()
              BillResponse(
                id = b.id,
                groupId = b.groupId,
                description = b.title,
                totalAmount = b.totalAmount,
                paidByUserId = b.paidBy,
                paidByUsername = b.paidByUsername,
                currency = b.currency,
                exchangeRate = b.exchangeRate,
                category = b.category ?: "OTHER",
                receiptUrl = b.receiptImageUrl,
                isPaid = b.isPaid,
                splits = splits,
                createdAt = b.createdAt ?: ""
              )
            }
            return Result.success(cachedResponse)
          }
        } catch (_: Exception) {}
      }
      Result.failure(e)
    }
  }

  /**
   * Lấy danh sách giao dịch gần nhất của người dùng trên tất cả các nhóm mà họ tham gia
   */
  suspend fun getRecentBills(limit: Int = 5): Result<List<BillResponse>> {
    return try {
      val cachedToken = tokenManager.getCachedToken()
      val sessionValid = SupabaseConfig.ensureSession(cachedToken, tokenManager)
      val currentUserId = auth.currentUserOrNull()?.id 
      if (!sessionValid || currentUserId == null || auth.currentSessionOrNull() == null) {
        tokenManager.deleteToken()
        return Result.failure(Exception("Chưa đăng nhập hoặc phiên đăng nhập đã hết hạn"))
      }

      // Lấy danh sách nhóm của user
      val memberRecords = postgrest["group_members"]
        .select { filter { eq("user_id", currentUserId) } }
        .decodeList<GroupMemberEntity>()
      
      val groupIds = memberRecords.map { it.groupId }
      if (groupIds.isEmpty()) {
        return Result.success(emptyList())
      }

      // Lấy các hóa đơn mới nhất từ các nhóm này
      val bills = postgrest["bills"]
        .select { 
          filter { isIn("group_id", groupIds) }
          order(column = "created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
          limit(limit.toLong())
        }
        .decodeList<BillEntity>()

      if (bills.isEmpty()) {
        return Result.success(emptyList())
      }

      val billIds = bills.mapNotNull { it.id }
      val splits = postgrest["bill_splits"]
        .select { filter { isIn("bill_id", billIds) } }
        .decodeList<BillSplitEntity>()

      val involvedUserIds = (bills.map { it.paidByUserId } + splits.map { it.userId }).filter { it.isNotBlank() }.distinct()
      val profiles = if (involvedUserIds.isNotEmpty()) {
        try {
          postgrest["profiles"]
            .select { filter { isIn("id", involvedUserIds) } }
            .decodeList<ProfileEntity>()
        } catch (_: Exception) {
          emptyList()
        }
      } else emptyList()
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

      // Cache xuống Room database để khởi động tức thì
      billDao?.let { dao ->
        val localBills = bills.map { b ->
          com.example.splitbill.data.local.entity.BillEntityLocal(
            id = b.id ?: "",
            groupId = b.groupId,
            paidBy = b.paidByUserId,
            paidByUsername = profileMap[b.paidByUserId]?.username ?: "Thành viên",
            title = b.description,
            description = b.description,
            totalAmount = b.totalAmount,
            currency = b.currency,
            exchangeRate = b.exchangeRate,
            category = b.category,
            receiptImageUrl = b.receiptUrl,
            isPaid = false,
            splitType = "EQUAL",
            createdAt = b.createdAt,
            isPendingSync = false
          )
        }
        val localSplits = splits.map { s ->
          com.example.splitbill.data.local.entity.BillSplitEntityLocal(
            billId = s.billId,
            userId = s.userId,
            username = profileMap[s.userId]?.username ?: "Thành viên",
            amountOwed = s.amountOwed,
            isSettled = false
          )
        }
        dao.insertBills(localBills)
        if (localSplits.isNotEmpty()) {
          dao.insertSplits(localSplits)
        }
      }

      Result.success(response)
    } catch (e: Exception) {
      if (SupabaseConfig.isAuthError(e)) {
        tokenManager.deleteToken()
      }
      // Fallback cache từ Room
      if (billDao != null) {
        try {
          val cachedBills = billDao.getRecentBills(limit)
          if (cachedBills.isNotEmpty()) {
            val billIds = cachedBills.map { it.id }
            val cachedSplits = billDao.getSplitsForBills(billIds).groupBy { it.billId }
            val cachedResponse = cachedBills.map { b ->
              val splits = cachedSplits[b.id]?.map { s ->
                BillSplitResponse(
                  userId = s.userId,
                  username = s.username,
                  amountOwed = s.amountOwed
                )
              } ?: emptyList()
              BillResponse(
                id = b.id,
                groupId = b.groupId,
                description = b.title,
                totalAmount = b.totalAmount,
                paidByUserId = b.paidBy,
                paidByUsername = b.paidByUsername,
                currency = b.currency,
                exchangeRate = b.exchangeRate,
                category = b.category ?: "OTHER",
                receiptUrl = b.receiptImageUrl,
                isPaid = b.isPaid,
                splits = splits,
                createdAt = b.createdAt ?: ""
              )
            }
            return Result.success(cachedResponse)
          }
        } catch (_: Exception) {}
      }
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
    category: String = "GENERAL",
    clientBillId: String? = null,
    knownUsernames: Map<String, String> = emptyMap()
  ): Result<BillResponse> {
    return try {
      val cachedToken = tokenManager.getCachedToken()
      val sessionValid = SupabaseConfig.ensureSession(cachedToken, tokenManager)
      val currentUserId = auth.currentUserOrNull()?.id ?: tokenManager.getUserId() ?: paidByUserId
      if (!sessionValid || auth.currentSessionOrNull() == null) {
        tokenManager.deleteToken()
        return Result.failure(Exception("Chưa đăng nhập hoặc phiên đăng nhập đã hết hạn"))
      }

      val billId = clientBillId ?: UUID.randomUUID().toString()

      val billEntity = BillEntity(
        id = billId,
        groupId = groupId,
        description = description,
        totalAmount = totalAmount,
        paidByUserId = paidByUserId,
        currency = currency,
        exchangeRate = exchangeRate,
        category = category
      )

      if (clientBillId != null) {
        postgrest["bills"].upsert(billEntity)
      } else {
        postgrest["bills"].insert(billEntity)
      }

      val splitEntities = splits.map {
        BillSplitEntity(
          billId = billId,
          userId = it.userId,
          amountOwed = it.amount
        )
      }

      if (splitEntities.isNotEmpty()) {
        if (clientBillId != null) {
          try {
            postgrest["bill_splits"].delete { filter { eq("bill_id", billId) } }
          } catch (_: Exception) {}
        }
        postgrest["bill_splits"].insert(splitEntities)
      }

      val profileMap = knownUsernames

      val splitResponses = splits.map {
        BillSplitResponse(
          userId = it.userId,
          username = profileMap[it.userId] ?: "User",
          amountOwed = it.amount
        )
      }

      // Ghi activity log bất đồng bộ ngầm hoàn toàn - KHÔNG chặn luồng phản hồi chính
      CoroutineScope(Dispatchers.IO).launch {
        try {
          val actorName = profileMap[currentUserId] ?: "Ai đó"
          postgrest["activity_logs"].insert(
            ActivityLogEntity(
              groupId = groupId,
              userId = currentUserId,
              activityType = "BILL_CREATED",
              description = "$actorName đã thêm hóa đơn '$description'|CAT:$category"
            )
          )
        } catch (_: Exception) {}
      }

      // Xóa cache thống kê để UI reload lại số dư mới nhất
      StatsRepository.clearCache()

      // Cập nhật ngay vào Room cache cục bộ
      billDao?.let { dao ->
        try {
          dao.insertBill(
            com.example.splitbill.data.local.entity.BillEntityLocal(
              id = billId,
              groupId = groupId,
              paidBy = paidByUserId,
              paidByUsername = profileMap[paidByUserId] ?: "User",
              title = description,
              description = description,
              totalAmount = totalAmount,
              currency = currency,
              exchangeRate = exchangeRate,
              category = category,
              receiptImageUrl = null,
              isPaid = false,
              createdAt = java.time.Instant.now().toString()
            )
          )
          dao.insertSplits(
            splits.map { s ->
              com.example.splitbill.data.local.entity.BillSplitEntityLocal(
                billId = billId,
                userId = s.userId,
                username = profileMap[s.userId] ?: "User",
                amountOwed = s.amount
              )
            }
          )
        } catch (_: Exception) {}
      }

      Result.success(
        BillResponse(
          id = billId,
          groupId = groupId,
          description = description,
          totalAmount = totalAmount,
          paidByUserId = paidByUserId,
          paidByUsername = profileMap[paidByUserId] ?: "User",
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
      if (SupabaseConfig.isAuthError(e)) {
        tokenManager.deleteToken()
      }
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
    category: String = "GENERAL",
    knownUsernames: Map<String, String> = emptyMap()
  ): Result<BillResponse> {
    return try {
      val cachedToken = tokenManager.getCachedToken()
      val sessionValid = SupabaseConfig.ensureSession(cachedToken, tokenManager)
      if (!sessionValid || auth.currentSessionOrNull() == null) {
        tokenManager.deleteToken()
        return Result.failure(Exception("Chưa đăng nhập hoặc phiên đăng nhập đã hết hạn"))
      }

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

      val profileMap = knownUsernames

      val splitResponses = splits.map {
        BillSplitResponse(
          userId = it.userId,
          username = profileMap[it.userId] ?: "User",
          amountOwed = it.amount
        )
      }

      val localBill = billDao?.getBillsForGroup("")?.firstOrNull { it.id == billId }
      val groupId = localBill?.groupId ?: ""
      val receiptUrl = localBill?.receiptImageUrl

      // Xóa cache thống kê để UI reload lại số dư mới
      StatsRepository.clearCache()

      // Cập nhật Room cache
      billDao?.let { dao ->
        try {
          if (localBill != null) {
            dao.insertBill(
              localBill.copy(
                description = description,
                title = description,
                totalAmount = totalAmount,
                paidBy = paidByUserId,
                paidByUsername = profileMap[paidByUserId] ?: localBill.paidByUsername,
                currency = currency,
                exchangeRate = exchangeRate,
                category = category
              )
            )
          }
          dao.deleteSplitsForBills(listOf(billId))
          dao.insertSplits(
            splits.map { s ->
              com.example.splitbill.data.local.entity.BillSplitEntityLocal(
                billId = billId,
                userId = s.userId,
                username = profileMap[s.userId] ?: "User",
                amountOwed = s.amount
              )
            }
          )
        } catch (_: Exception) {}
      }

      Result.success(
        BillResponse(
          id = billId,
          groupId = groupId,
          description = description,
          totalAmount = totalAmount,
          paidByUserId = paidByUserId,
          paidByUsername = profileMap[paidByUserId] ?: "User",
          currency = currency,
          exchangeRate = exchangeRate,
          category = category,
          receiptUrl = receiptUrl,
          isPaid = localBill?.isPaid ?: false,
          splits = splitResponses,
          createdAt = localBill?.createdAt ?: java.time.Instant.now().toString()
        )
      )
    } catch (e: Exception) {
      if (SupabaseConfig.isAuthError(e)) {
        tokenManager.deleteToken()
      }
      Result.failure(e)
    }
  }

  suspend fun deleteBill(billId: String): Result<Unit> {
    return try {
      val cachedToken = tokenManager.getCachedToken()
      SupabaseConfig.ensureSession(cachedToken, tokenManager)
      postgrest["bills"].delete { filter { eq("id", billId) } }
      billDao?.deleteBill(billId)
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun uploadReceipt(billId: String, imageBytes: ByteArray): Result<String> {
    return try {
      val cachedToken = tokenManager.getCachedToken()
      SupabaseConfig.ensureSession(cachedToken, tokenManager)
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
