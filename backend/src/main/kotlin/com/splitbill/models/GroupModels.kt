package com.splitbill.models

import kotlinx.serialization.Serializable

// ==========================================
// REQUEST DTOs
// ==========================================

@Serializable
data class CreateGroupRequest(
    val name: String
)

@Serializable
data class AddMemberRequest(
    val usernameOrEmail: String // Tìm thành viên bằng username hoặc email
)

// ==========================================
// RESPONSE DTOs
// ==========================================

@Serializable
data class GroupResponse(
    val id: String,
    val name: String,
    val createdBy: String,      // userId của người tạo
    val createdByName: String,  // username của người tạo
    val memberCount: Int,
    val createdAt: String
)

@Serializable
data class MemberResponse(
    val userId: String,
    val username: String,
    val email: String,
    val joinedAt: String
)

@Serializable
data class MessageResponse(
    val message: String
)
