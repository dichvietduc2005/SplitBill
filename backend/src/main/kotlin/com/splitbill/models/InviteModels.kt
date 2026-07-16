package com.splitbill.models

import kotlinx.serialization.Serializable

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
