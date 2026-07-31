package com.example.splitbill.data.api

import kotlinx.serialization.Serializable

// ==========================================
// AUTH
// ==========================================

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class RegisterRequest(val username: String, val email: String, val password: String)

@Serializable
data class AuthResponse(val token: String)

@Serializable
data class RegisterFcmTokenRequest(val token: String)

// ==========================================
// GROUP
// ==========================================

@Serializable
data class GroupResponse(
  val id: String,
  val name: String,
  val createdBy: String,
  val createdByName: String,
  val memberCount: Int,
  val createdAt: String
)

@Serializable
data class CreateGroupRequest(val name: String)

@Serializable
data class AddMemberRequest(val usernameOrEmail: String)

@Serializable
data class MemberResponse(
  val userId: String,
  val username: String,
  val email: String,
  val avatarUrl: String? = null,
  val joinedAt: String
)

// ==========================================
// BILL
// ==========================================

@Serializable
data class BillResponse(
  val id: String,
  val groupId: String,
  val description: String,
  val totalAmount: Double,
  val paidByUserId: String,
  val paidByUsername: String,
  val currency: String,
  val exchangeRate: Double,
  val receiptUrl: String? = null,
  val isPaid: Boolean = false,
  val splits: List<BillSplitResponse>,
  val createdAt: String
)
@Serializable
data class PaginatedBillResponse(
  val data: List<BillResponse>,
  val total: Long,
  val limit: Int,
  val offset: Int
)

@Serializable
data class BillSplitResponse(
  val userId: String,
  val username: String,
  val amountOwed: Double
)

@Serializable
data class CreateBillRequest(
  val groupId: String,
  val description: String,
  val totalAmount: Double,
  val paidByUserId: String,
  val currency: String = "VND",
  val exchangeRate: Double = 1.0,
  val splits: List<BillSplitItem>
)

@Serializable
data class BillSplitItem(
  val userId: String,
  val amount: Double
)

@Serializable
data class UpdateBillRequest(
  val description: String,
  val totalAmount: Double,
  val paidByUserId: String,
  val currency: String = "VND",
  val exchangeRate: Double = 1.0,
  val splits: List<BillSplitItem>
)

// ==========================================
// DEBT
// ==========================================

@Serializable
data class DebtResponse(
  val groupId: String,
  val groupName: String,
  val debts: List<SimplifiedDebt>,
  val totalTransactions: Int
)

@Serializable
data class SimplifiedDebt(
  val fromUserId: String,
  val fromUsername: String,
  val toUserId: String,
  val toUsername: String,
  val amount: Double
)

// ==========================================
// PROFILE & VIETQR
// ==========================================

@Serializable
data class ProfileResponse(
  val id: String,
  val username: String,
  val email: String,
  val avatarUrl: String? = null,
  val bankCode: String? = null,
  val accountNumber: String? = null,
  val accountName: String? = null
)

@Serializable
data class UpdateBankInfoRequest(
  val bankCode: String,
  val accountNumber: String,
  val accountName: String
)

// ==========================================
// SETTLEMENT
// ==========================================

@Serializable
data class CreateSettlementRequest(
  val groupId: String,
  val toUserId: String,
  val amount: Double,
  val note: String? = null,
  val fromUserId: String? = null
)

@Serializable
data class SettlementResponse(
  val id: String,
  val groupId: String,
  val fromUserId: String,
  val fromUsername: String,
  val toUserId: String,
  val toUsername: String,
  val amount: Double,
  val note: String?,
  val createdAt: String
)

// ==========================================
// GROUP INVITES
// ==========================================

@Serializable
data class CreateInviteRequest(
  val maxUses: Int? = null
)

@Serializable
data class InviteResponse(
  val id: String,
  val groupId: String,
  val groupName: String,
  val inviteCode: String,
  val inviteUrl: String,
  val expiresAt: String,
  val maxUses: Int?,
  val useCount: Int,
  val createdAt: String
)

@Serializable
data class JoinGroupWithCodeRequest(
  val inviteCode: String
)

// ==========================================
// STATS
// ==========================================

@Serializable
data class GroupSpent(
  val groupId: String,
  val groupName: String,
  val amount: Double
)

@Serializable
data class MonthlySpent(
  val month: String,
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

@Serializable
data class ActivityResponse(
  val id: String,
  val groupId: String,
  val userId: String,
  val username: String,
  val activityType: String,
  val description: String,
  val createdAt: String
)

@Serializable
data class PaginatedActivityResponse(
  val data: List<ActivityResponse>,
  val total: Long,
  val limit: Int,
  val offset: Int
)
