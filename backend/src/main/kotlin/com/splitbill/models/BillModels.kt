package com.splitbill.models

import kotlinx.serialization.Serializable

// ==========================================
// REQUEST DTOs
// ==========================================

@Serializable
data class CreateBillRequest(
    val groupId: String,
    val description: String,
    val totalAmount: Double,
    val paidByUserId: String,       // UUID của người đã trả tiền
    val splits: List<BillSplitItem> // Danh sách ai nợ bao nhiêu
)

@Serializable
data class BillSplitItem(
    val userId: String,
    val amount: Double  // Số tiền mà user này nợ
)

// ==========================================
// RESPONSE DTOs
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
data class BillSplitResponse(
    val userId: String,
    val username: String,
    val amountOwed: Double
)

// ==========================================
// DEBT SIMPLIFICATION RESPONSE
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
    val amount: Double  // Số tiền fromUser cần trả cho toUser
)
