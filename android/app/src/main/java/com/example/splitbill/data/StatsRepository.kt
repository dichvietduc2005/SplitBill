package com.example.splitbill.data

import com.example.splitbill.data.api.*
import com.example.splitbill.data.supabase.*
import com.example.splitbill.utils.DebtSimplifier
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class StatsRepository(private val tokenManager: TokenManager) {

  companion object {
    @Volatile private var cachedStats: UserStatsResponse? = null
    fun getCachedStats(): UserStatsResponse? = cachedStats
    fun clearCache() { cachedStats = null }
  }

  private val postgrest get() = SupabaseConfig.client.postgrest
  private val auth get() = SupabaseConfig.client.auth

  suspend fun getUserStats(): Result<UserStatsResponse> {
    return try {
      val cachedToken = tokenManager.getCachedToken()
      val sessionValid = SupabaseConfig.ensureSession(cachedToken, tokenManager)
      val userId = auth.currentUserOrNull()?.id
      if (!sessionValid || userId == null || auth.currentSessionOrNull() == null) {
        tokenManager.deleteToken()
        return Result.failure(Exception("Chưa đăng nhập hoặc phiên đăng nhập đã hết hạn"))
      }

      val memberRecords = postgrest["group_members"]
        .select { filter { eq("user_id", userId) } }
        .decodeList<GroupMemberEntity>()

      if (memberRecords.isEmpty()) {
        return Result.success(
          UserStatsResponse(0.0, 0.0, 0.0, emptyList(), emptyList())
        )
      }

      val groupIds = memberRecords.map { it.groupId }

      // Tải song song danh sách nhóm, hóa đơn và thanh quyết toán
      val (groups, allBills, allSettlements) = coroutineScope {
        val groupsDeferred = async {
          postgrest["groups"]
            .select { filter { isIn("id", groupIds) } }
            .decodeList<GroupEntity>()
        }
        val allBillsDeferred = async {
          postgrest["bills"]
            .select { filter { isIn("group_id", groupIds) } }
            .decodeList<BillEntity>()
        }
        val allSettlementsDeferred = async {
          postgrest["settlements"]
            .select { filter { isIn("group_id", groupIds) } }
            .decodeList<SettlementEntity>()
        }
        Triple(groupsDeferred.await(), allBillsDeferred.await(), allSettlementsDeferred.await())
      }
      val groupMap = groups.associate { it.id to it.name }

      val billIds = allBills.mapNotNull { it.id }
      val allSplits = if (billIds.isNotEmpty()) {
        postgrest["bill_splits"]
          .select { filter { isIn("bill_id", billIds) } }
          .decodeList<BillSplitEntity>()
      } else emptyList()

      // Chỉ lấy hồ sơ của các thành viên có liên quan thay vì tải toàn bộ bảng profiles
      val involvedUserIds = (
        allBills.map { it.paidByUserId } +
        allSplits.map { it.userId } +
        allSettlements.map { it.fromUserId } +
        allSettlements.map { it.toUserId } +
        listOf(userId)
      ).filter { it.isNotBlank() }.distinct()

      val profiles = if (involvedUserIds.isNotEmpty()) {
        try {
          postgrest["profiles"]
            .select { filter { isIn("id", involvedUserIds) } }
            .decodeList<ProfileEntity>()
        } catch (_: Exception) {
          emptyList()
        }
      } else emptyList()
      val profileMap = profiles.associate { it.id to it.username }

      var totalSpent = 0.0
      var totalOwedToOthers = 0.0
      var totalOthersOweToMe = 0.0

      val spentByGroupMap = mutableMapOf<String, Double>()
      val monthlyTrendMap = mutableMapOf<String, Double>()

      val splitsByBillId = allSplits.groupBy { it.billId }

      val groupBalanceMap = mutableMapOf<String, Double>()
      for (group in groups) {
        val gBills = allBills.filter { it.groupId == group.id }
        val gSettlements = allSettlements.filter { it.groupId == group.id }

        // Tính chi tiêu nhóm và của user
        val groupBillsTotal = gBills.sumOf {
          val rate = if (it.exchangeRate > 0) it.exchangeRate else 1.0
          it.totalAmount * rate
        }
        val gBillIds = gBills.mapNotNull { it.id }.toSet()
        val userSplits = allSplits.filter { gBillIds.contains(it.billId) && it.userId == userId }
        val userGroupSpent = userSplits.sumOf { it.amountOwed }
        totalSpent += userGroupSpent

        // Thẻ nhóm hiển thị tổng chi tiêu toàn nhóm nếu có hóa đơn, nếu không thì phần chi của user
        spentByGroupMap[group.id] = if (groupBillsTotal > 0) groupBillsTotal else userGroupSpent

        // Monthly trend
        for (split in userSplits) {
          val bill = gBills.find { it.id == split.billId }
          if (bill != null && (bill.createdAt?.length ?: 0) >= 7) {
            val created = bill.createdAt!!
            val month = created.substring(5, 7) + "/" + created.substring(0, 4)
            monthlyTrendMap[month] = (monthlyTrendMap[month] ?: 0.0) + split.amountOwed
          }
        }

        // Tính nợ chuẩn xác cho từng nhóm bằng DebtSimplifier
        var groupNetBalance = 0.0
        if (gBills.isNotEmpty()) {
          val billResponses = gBills.map { b ->
            val bSplits = splitsByBillId[b.id] ?: emptyList()
            BillResponse(
              id = b.id ?: "",
              groupId = b.groupId,
              description = b.description,
              totalAmount = b.totalAmount,
              paidByUserId = b.paidByUserId,
              paidByUsername = profileMap[b.paidByUserId] ?: "User",
              currency = b.currency,
              exchangeRate = b.exchangeRate,
              splits = bSplits.map {
                BillSplitResponse(it.userId, profileMap[it.userId] ?: "User", it.amountOwed)
              },
              createdAt = b.createdAt ?: ""
            )
          }

          val settlementResponses = gSettlements.map { s ->
            SettlementResponse(
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

          val simplified = DebtSimplifier.simplify(billResponses, settlementResponses, profileMap)
          for (d in simplified) {
            if (d.fromUserId == userId) {
              totalOwedToOthers += d.amount
              groupNetBalance -= d.amount
            } else if (d.toUserId == userId) {
              totalOthersOweToMe += d.amount
              groupNetBalance += d.amount
            }
          }
        }
        groupBalanceMap[group.id] = Math.round(groupNetBalance * 100.0) / 100.0
      }

      val spentByGroupList = spentByGroupMap.map { (groupId, amount) ->
        GroupSpent(
          groupId = groupId,
          groupName = groupMap[groupId] ?: "Nhóm",
          amount = Math.round(amount * 100.0) / 100.0
        )
      }.sortedByDescending { it.amount }

      val monthlyTrendList = monthlyTrendMap.map { (month, amount) ->
        MonthlySpent(
          month = month,
          amount = Math.round(amount * 100.0) / 100.0
        )
      }.sortedBy { it.month }

      val response = UserStatsResponse(
        totalSpent = Math.round(totalSpent * 100.0) / 100.0,
        totalOwedToOthers = Math.round(totalOwedToOthers * 100.0) / 100.0,
        totalOthersOweToMe = Math.round(totalOthersOweToMe * 100.0) / 100.0,
        spentByGroup = spentByGroupList,
        monthlyTrend = monthlyTrendList,
        groupBalances = groupBalanceMap
      )
      cachedStats = response
      Result.success(response)
    } catch (e: Exception) {
      if (SupabaseConfig.isAuthError(e)) {
        tokenManager.deleteToken()
      }
      cachedStats?.let { return Result.success(it) }
      Result.failure(e)
    }
  }

  suspend fun getGroupStats(groupId: String): Result<GroupStatsResponse> {
    return try {
      val cachedToken = tokenManager.getCachedToken()
      val sessionValid = SupabaseConfig.ensureSession(cachedToken, tokenManager)
      val userId = auth.currentUserOrNull()?.id
      if (!sessionValid || userId == null || auth.currentSessionOrNull() == null) {
        tokenManager.deleteToken()
        return Result.failure(Exception("Chưa đăng nhập hoặc phiên đăng nhập đã hết hạn"))
      }

      val (bills, memberRecords) = coroutineScope {
        val billsDeferred = async {
          postgrest["bills"]
            .select { filter { eq("group_id", groupId) } }
            .decodeList<BillEntity>()
        }
        val membersDeferred = async {
          postgrest["group_members"]
            .select { filter { eq("group_id", groupId) } }
            .decodeList<GroupMemberEntity>()
        }
        Pair(billsDeferred.await(), membersDeferred.await())
      }

      val billIds = bills.mapNotNull { it.id }
      val splits = if (billIds.isNotEmpty()) {
        postgrest["bill_splits"]
          .select { filter { isIn("bill_id", billIds) } }
          .decodeList<BillSplitEntity>()
      } else emptyList()

      val userIds = memberRecords.map { it.userId }
      val profiles = if (userIds.isNotEmpty()) {
        try {
          postgrest["profiles"]
            .select { filter { isIn("id", userIds) } }
            .decodeList<ProfileEntity>()
        } catch (_: Exception) {
          emptyList()
        }
      } else emptyList()
      val profileMap = profiles.associate { it.id to it.username }

      var groupTotalSpent = 0.0
      var userPaid = 0.0
      var userOwed = 0.0

      val memberSpendingMap = mutableMapOf<String, Double>()
      val monthlyTrendMap = mutableMapOf<String, Double>()

      for (bill in bills) {
        val rate = if (bill.exchangeRate > 0) bill.exchangeRate else 1.0
        val amountInVnd = bill.totalAmount * rate
        groupTotalSpent += amountInVnd

        memberSpendingMap[bill.paidByUserId] = (memberSpendingMap[bill.paidByUserId] ?: 0.0) + amountInVnd
        if (bill.paidByUserId == userId) {
          userPaid += amountInVnd
        }

        if ((bill.createdAt?.length ?: 0) >= 7) {
          val created = bill.createdAt!!
          val month = created.substring(5, 7) + "/" + created.substring(0, 4)
          monthlyTrendMap[month] = (monthlyTrendMap[month] ?: 0.0) + amountInVnd
        }
      }

      val userSplits = splits.filter { it.userId == userId }
      for (split in userSplits) {
        val bill = bills.find { it.id == split.billId }
        val rate = if ((bill?.exchangeRate ?: 1.0) > 0) bill!!.exchangeRate else 1.0
        userOwed += split.amountOwed * rate
      }

      val memberSpendingList = memberRecords.map { member ->
        MemberSpent(
          userId = member.userId,
          username = profileMap[member.userId] ?: "User",
          amount = Math.round((memberSpendingMap[member.userId] ?: 0.0) * 100.0) / 100.0
        )
      }.sortedByDescending { it.amount }

      val monthlyTrendList = monthlyTrendMap.map { (month, amount) ->
        MonthlySpent(
          month = month,
          amount = Math.round(amount * 100.0) / 100.0
        )
      }.sortedBy { it.month }

      Result.success(
        GroupStatsResponse(
          totalSpent = Math.round(groupTotalSpent * 100.0) / 100.0,
          userSpent = Math.round(userPaid * 100.0) / 100.0,
          userOwed = Math.round(userOwed * 100.0) / 100.0,
          memberSpending = memberSpendingList,
          monthlyTrend = monthlyTrendList
        )
      )
    } catch (e: Exception) {
      if (SupabaseConfig.isAuthError(e)) {
        tokenManager.deleteToken()
      }
      Result.failure(e)
    }
  }
}
