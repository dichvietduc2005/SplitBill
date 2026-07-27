package com.splitbill.models

import kotlinx.serialization.Serializable

// ==========================================
// REQUEST DTOs
// ==========================================

@Serializable
data class CreateSettlementRequest(
    val groupId: String,
    val toUserId: String, // Trả nợ cho ai
    val amount: Double,
    val note: String? = null,
    val fromUserId: String? = null // Optional: người trả nợ (nếu creditor ghi nhận hộ)
)

// ==========================================
// RESPONSE DTOs
// ==========================================

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
