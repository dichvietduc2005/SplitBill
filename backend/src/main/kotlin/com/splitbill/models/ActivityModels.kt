package com.splitbill.models

import kotlinx.serialization.Serializable

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
