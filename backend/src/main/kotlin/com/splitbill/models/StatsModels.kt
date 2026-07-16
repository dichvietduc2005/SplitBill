package com.splitbill.models

import kotlinx.serialization.Serializable

@Serializable
data class GroupSpent(
    val groupId: String,
    val groupName: String,
    val amount: Double
)

@Serializable
data class MonthlySpent(
    val month: String, // Định dạng: "MM/yyyy"
    val amount: Double
)

@Serializable
data class UserStatsResponse(
    val totalSpent: Double,
    val totalOwedToOthers: Double,
    val totalOthersOweToMe: Double,
    val spentByGroup: List<GroupSpent>,
    val monthlyTrend: List<MonthlySpent>
)

@Serializable
data class MemberSpent(
    val userId: String,
    val username: String,
    val amount: Double
)

@Serializable
data class GroupStatsResponse(
    val totalSpent: Double,
    val userSpent: Double,
    val userOwed: Double,
    val memberSpending: List<MemberSpent>,
    val monthlyTrend: List<MonthlySpent>
)
