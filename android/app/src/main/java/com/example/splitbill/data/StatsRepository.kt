package com.example.splitbill.data

import com.example.splitbill.data.api.*
import com.example.splitbill.data.supabase.*
import com.example.splitbill.utils.DebtSimplifier
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

class StatsRepository(private val tokenManager: TokenManager) {

  private val postgrest get() = SupabaseConfig.client.postgrest
  private val auth get() = SupabaseConfig.client.auth

  suspend fun getUserStats(): Result<UserStatsResponse> {
    return try {
      val userId = auth.currentUserOrNull()?.id
        ?: TokenManager.getUserIdFromToken(tokenManager.getCachedToken())
        ?: return Result.failure(Exception("Chưa đăng nhập"))

      val memberRecords = postgrest["group_members"]
        .select { filter { eq("user_id", userId) } }
        .decodeList<GroupMemberEntity>()

      if (memberRecords.isEmpty()) {
        return Result.success(
          UserStatsResponse(0.0, 0.0, 0.0, emptyList(), emptyList())
        )
      }

      val groupIds = memberRecords.map { it.groupId }
      val groups = postgrest["groups"]
        .select { filter { isIn("id", groupIds) } }
        .decodeList<GroupEntity>()
      val groupMap = groups.associate { it.id to it.name }

      val allBills = postgrest["bills"]
        .select { filter { isIn("group_id", groupIds) } }
        .decodeList<BillEntity>()

      val billIds = allBills.mapNotNull { it.id }
      val allSplits = if (billIds.isNotEmpty()) {
        postgrest["bill_splits"]
          .select { filter { isIn("bill_id", billIds) } }
          .decodeList<BillSplitEntity>()
      } else emptyList()

      val allSettlements = postgrest["settlements"]
        .select { filter { isIn("group_id", groupIds) } }
        .decodeList<SettlementEntity>()

      val profiles = postgrest["profiles"]
        .select()
        .decodeList<ProfileEntity>()
      val profileMap = profiles.associate { it.id to it.username }

      var totalSpent = 0.0
      var totalOwedToOthers = 0.0
      var totalOthersOweToMe = 0.0

      val spentByGroupMap = mutableMapOf<String, Double>()
      val monthlyTrendMap = mutableMapOf<String, Double>()

      val splitsByBillId = allSplits.groupBy { it.billId }

      for (group in groups) {
        val gBills = allBills.filter { it.groupId == group.id }
        val gSettlements = allSettlements.filter { it.groupId == group.id }

        // Tính chi tiêu
        val gBillIds = gBills.mapNotNull { it.id }.toSet()
        val userSplits = allSplits.filter { gBillIds.contains(it.billId) && it.userId == userId }
        val groupTotalSpent = userSplits.sumOf { it.amountOwed }
        if (groupTotalSpent > 0) {
          spentByGroupMap[group.id] = (spentByGroupMap[group.id] ?: 0.0) + groupTotalSpent
          totalSpent += groupTotalSpent
        }

        // Monthly trend
        for (split in userSplits) {
          val bill = gBills.find { it.id == split.billId }
          if (bill != null && (bill.createdAt?.length ?: 0) >= 7) {
            val created = bill.createdAt!!
            val month = created.substring(5, 7) + "/" + created.substring(0, 4)
            monthlyTrendMap[month] = (monthlyTrendMap[month] ?: 0.0) + split.amountOwed
          }
        }

        // Tính nợ bằng DebtSimplifier
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
            } else if (d.toUserId == userId) {
              totalOthersOweToMe += d.amount
            }
          }
        }
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

      Result.success(
        UserStatsResponse(
          totalSpent = Math.round(totalSpent * 100.0) / 100.0,
          totalOwedToOthers = Math.round(totalOwedToOthers * 100.0) / 100.0,
          totalOthersOweToMe = Math.round(totalOthersOweToMe * 100.0) / 100.0,
          spentByGroup = spentByGroupList,
          monthlyTrend = monthlyTrendList
        )
      )
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun getGroupStats(groupId: String): Result<GroupStatsResponse> {
    return try {
      val userId = auth.currentUserOrNull()?.id
        ?: TokenManager.getUserIdFromToken(tokenManager.getCachedToken())
        ?: return Result.failure(Exception("Chưa đăng nhập"))

      val bills = postgrest["bills"]
        .select { filter { eq("group_id", groupId) } }
        .decodeList<BillEntity>()

      val billIds = bills.mapNotNull { it.id }
      val splits = if (billIds.isNotEmpty()) {
        postgrest["bill_splits"]
          .select { filter { isIn("bill_id", billIds) } }
          .decodeList<BillSplitEntity>()
      } else emptyList()

      val memberRecords = postgrest["group_members"]
        .select { filter { eq("group_id", groupId) } }
        .decodeList<GroupMemberEntity>()

      val userIds = memberRecords.map { it.userId }
      val profiles = if (userIds.isNotEmpty()) {
        postgrest["profiles"]
          .select { filter { isIn("id", userIds) } }
          .decodeList<ProfileEntity>()
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
      Result.failure(e)
    }
  }
}
