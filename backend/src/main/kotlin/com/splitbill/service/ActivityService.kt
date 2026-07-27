package com.splitbill.service

import com.splitbill.data.ActivityRepository
import com.splitbill.data.GroupRepository
import com.splitbill.exceptions.ForbiddenException
import com.splitbill.models.ActivityResponse
import com.splitbill.models.PaginatedActivityResponse

class ActivityService(
    private val activityRepository: ActivityRepository,
    private val groupRepository: GroupRepository
) {

    suspend fun getActivitiesForGroup(
        groupId: String,
        userId: String,
        limit: Int = 50,
        offset: Int = 0
    ): PaginatedActivityResponse {
        // Kiểm tra xem người dùng có phải thành viên nhóm không
        if (!groupRepository.isMember(groupId, userId)) {
            throw ForbiddenException("Bạn không phải thành viên nhóm này")
        }

        val logs = activityRepository.getActivitiesForGroup(groupId, limit, offset)
        val total = activityRepository.countActivitiesForGroup(groupId)

        val data = logs.map { log ->
            ActivityResponse(
                id = log.id,
                groupId = log.groupId,
                userId = log.userId,
                username = log.username,
                activityType = log.activityType,
                description = log.description,
                createdAt = log.createdAt
            )
        }

        return PaginatedActivityResponse(
            data = data,
            total = total,
            limit = limit,
            offset = offset
        )
    }
}
