package com.splitbill.service

import com.splitbill.data.*
import com.splitbill.models.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class StatsService(
    private val groupRepository: GroupRepository,
    private val billRepository: BillRepository,
    private val settlementRepository: SettlementRepository
) {

    suspend fun getUserStats(userId: String): UserStatsResponse {
        val groups = groupRepository.getGroupsForUser(userId)
        
        var totalSpent = 0.0
        var totalOwedToOthers = 0.0
        var totalOthersOweToMe = 0.0
        
        val spentByGroupMap = mutableMapOf<String, Double>() // groupId -> amount
        val groupNames = mutableMapOf<String, String>() // groupId -> name
        val monthlyTrendMap = mutableMapOf<String, Double>() // "MM/yyyy" -> amount

        for (group in groups) {
            val groupId = group.id
            groupNames[groupId] = group.name

            val bills = billRepository.getAllBillsForGroup(groupId)
            val allSplits = billRepository.getAllSplitsForGroup(groupId)
            val settlements = settlementRepository.getSettlementsForGroup(groupId)
            
            // 1. Tính chi tiêu thực tế (Splits của user này)
            val userSplits = allSplits.filter { it.userId == userId }
            val groupTotalSpent = userSplits.sumOf { it.amountOwed.toDouble() }
            if (groupTotalSpent > 0) {
                spentByGroupMap[groupId] = (spentByGroupMap[groupId] ?: 0.0) + groupTotalSpent
                totalSpent += groupTotalSpent
            }

            // Gom nhóm theo tháng cho trend
            for (split in userSplits) {
                val bill = bills.find { it.id == split.billId }
                if (bill != null && bill.createdAt.length >= 7) {
                    val month = bill.createdAt.substring(5, 7) + "/" + bill.createdAt.substring(0, 4)
                    monthlyTrendMap[month] = (monthlyTrendMap[month] ?: 0.0) + split.amountOwed.toDouble()
                }
            }

            // 2. Tính nợ / được nợ dựa trên thuật toán tối giản (có tính cả settlements)
            if (bills.isNotEmpty()) {
                val members = groupRepository.getMembers(groupId)
                val memberMap = members.associate { it.userId to it.username }
                val simplifiedDebts = DebtSimplifier.simplify(bills, allSplits, settlements, memberMap)
                
                for (debt in simplifiedDebts) {
                    if (debt.fromUserId == userId) {
                        // User đang nợ người khác
                        totalOwedToOthers += debt.amount
                    } else if (debt.toUserId == userId) {
                        // Người khác đang nợ User
                        totalOthersOweToMe += debt.amount
                    }
                }
            }
        }

        val spentByGroupList = spentByGroupMap.map { (groupId, amount) ->
            GroupSpent(
                groupId = groupId,
                groupName = groupNames[groupId] ?: "Unknown",
                amount = Math.round(amount * 100.0) / 100.0
            )
        }.sortedByDescending { it.amount }

        val monthlyTrendList = monthlyTrendMap.map { (month, amount) ->
            MonthlySpent(
                month = month,
                amount = Math.round(amount * 100.0) / 100.0
            )
        }.sortedBy { 
            // Sắp xếp theo thứ tự thời gian (yyyyMM)
            val parts = it.month.split("/")
            if (parts.size == 2) parts[1] + parts[0] else it.month
        }

        return UserStatsResponse(
            totalSpent = Math.round(totalSpent * 100.0) / 100.0,
            totalOwedToOthers = Math.round(totalOwedToOthers * 100.0) / 100.0,
            totalOthersOweToMe = Math.round(totalOthersOweToMe * 100.0) / 100.0,
            spentByGroup = spentByGroupList,
            monthlyTrend = monthlyTrendList
        )
    }

    suspend fun getGroupStats(groupId: String, userId: String): GroupStatsResponse {
        if (!groupRepository.isMember(groupId, userId)) {
            throw com.splitbill.exceptions.ForbiddenException("Bạn không phải thành viên nhóm này")
        }

        val bills = billRepository.getAllBillsForGroup(groupId)
        val allSplits = billRepository.getAllSplitsForGroup(groupId)
        val members = groupRepository.getMembers(groupId)

        var groupTotalSpent = 0.0
        var userPaid = 0.0
        var userOwed = 0.0

        val memberSpendingMap = mutableMapOf<String, Double>()
        val monthlyTrendMap = mutableMapOf<String, Double>()

        for (bill in bills) {
            val rate = bill.exchangeRate.toDouble()
            val amountInVnd = bill.totalAmount.toDouble() * rate
            groupTotalSpent += amountInVnd

            memberSpendingMap[bill.paidByUserId] = (memberSpendingMap[bill.paidByUserId] ?: 0.0) + amountInVnd
            if (bill.paidByUserId == userId) {
                userPaid += amountInVnd
            }
        }

        val userSplits = allSplits.filter { it.userId == userId }
        for (split in userSplits) {
            val bill = bills.find { it.id == split.billId }
            val rate = bill?.exchangeRate?.toDouble() ?: 1.0
            val amountInVnd = split.amountOwed.toDouble() * rate
            userOwed += amountInVnd
        }

        for (bill in bills) {
            if (bill.createdAt.length >= 7) {
                val rate = bill.exchangeRate.toDouble()
                val amountInVnd = bill.totalAmount.toDouble() * rate
                val month = bill.createdAt.substring(5, 7) + "/" + bill.createdAt.substring(0, 4)
                monthlyTrendMap[month] = (monthlyTrendMap[month] ?: 0.0) + amountInVnd
            }
        }

        val memberSpendingList = members.map { member ->
            MemberSpent(
                userId = member.userId,
                username = member.username,
                amount = Math.round((memberSpendingMap[member.userId] ?: 0.0) * 100.0) / 100.0
            )
        }.sortedByDescending { it.amount }

        val monthlyTrendList = monthlyTrendMap.map { (month, amount) ->
            MonthlySpent(
                month = month,
                amount = Math.round(amount * 100.0) / 100.0
            )
        }.sortedBy {
            val parts = it.month.split("/")
            if (parts.size == 2) parts[1] + parts[0] else it.month
        }

        return GroupStatsResponse(
            totalSpent = Math.round(groupTotalSpent * 100.0) / 100.0,
            userSpent = Math.round(userPaid * 100.0) / 100.0,
            userOwed = Math.round(userOwed * 100.0) / 100.0,
            memberSpending = memberSpendingList,
            monthlyTrend = monthlyTrendList
        )
    }
}
