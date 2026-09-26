package com.example.splitbill.data.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileEntity(
    val id: String,
    val email: String,
    val username: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("bank_code") val bankCode: String? = null,
    @SerialName("account_number") val accountNumber: String? = null,
    @SerialName("account_name") val accountName: String? = null
)

@Serializable
data class GroupEntity(
    val id: String,
    val name: String,
    val description: String? = "",
    val icon: String? = "default",
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class GroupMemberEntity(
    val id: String? = null,
    @SerialName("group_id") val groupId: String,
    @SerialName("user_id") val userId: String,
    val role: String = "MEMBER",
    @SerialName("joined_at") val joinedAt: String? = null
)

@Serializable
data class BillEntity(
    val id: String? = null,
    @SerialName("group_id") val groupId: String,
    val description: String,
    @SerialName("total_amount") val totalAmount: Double,
    @SerialName("paid_by_user_id") val paidByUserId: String,
    val currency: String = "VND",
    @SerialName("exchange_rate") val exchangeRate: Double = 1.0,
    val category: String = "GENERAL",
    @SerialName("receipt_url") val receiptUrl: String? = null,
    @SerialName("is_paid") val isPaid: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class BillSplitEntity(
    val id: String? = null,
    @SerialName("bill_id") val billId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("amount_owed") val amountOwed: Double,
    @SerialName("is_settled") val isSettled: Boolean = false
)

@Serializable
data class SettlementEntity(
    val id: String? = null,
    @SerialName("group_id") val groupId: String,
    @SerialName("from_user_id") val fromUserId: String,
    @SerialName("to_user_id") val toUserId: String,
    val amount: Double,
    val note: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ActivityLogEntity(
    val id: String? = null,
    @SerialName("group_id") val groupId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("activity_type") val activityType: String,
    val description: String,
    @SerialName("created_at") val createdAt: String? = null
)
