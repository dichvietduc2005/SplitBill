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
  val splits: List<BillSplitItem>
)

@Serializable
data class BillSplitItem(
  val userId: String,
  val amount: Double
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
