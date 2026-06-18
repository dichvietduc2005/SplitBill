package com.splitbill.service

import com.splitbill.data.GroupRepository
import com.splitbill.data.UserRepository
import com.splitbill.exceptions.ForbiddenException
import com.splitbill.exceptions.InternalException
import com.splitbill.exceptions.NotFoundException
import com.splitbill.models.*

/**
 * GroupService — chứa logic nghiệp vụ quản lý nhóm:
 * - Tạo nhóm, lấy danh sách nhóm, lấy chi tiết nhóm
 * - Kiểm tra quyền thành viên, thêm thành viên
 */
class GroupService(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository
) {

    suspend fun createGroup(name: String, userId: String): GroupResponse {
        val group = groupRepository.createGroup(name, userId)
            ?: throw InternalException("Lỗi server khi tạo nhóm")

        val creatorUser = userRepository.findUserById(userId)
        return GroupResponse(
            id = group.id,
            name = group.name,
            createdBy = group.createdBy,
            createdByName = creatorUser?.username ?: "Unknown",
            memberCount = 1,
            createdAt = group.createdAt
        )
    }

    suspend fun getGroupsForUser(userId: String): List<GroupResponse> {
        val groups = groupRepository.getGroupsForUser(userId)
        return groups.map { group ->
            val creatorUser = userRepository.findUserById(group.createdBy)
            val memberCount = groupRepository.getMemberCount(group.id)
            GroupResponse(
                id = group.id,
                name = group.name,
                createdBy = group.createdBy,
                createdByName = creatorUser?.username ?: "Unknown",
                memberCount = memberCount,
                createdAt = group.createdAt
            )
        }
    }

    suspend fun getGroupDetail(groupId: String, userId: String): GroupResponse {
        ensureMember(groupId, userId)

        val group = groupRepository.getGroupById(groupId)
            ?: throw NotFoundException("Không tìm thấy nhóm")

        val creatorUser = userRepository.findUserById(group.createdBy)
        val memberCount = groupRepository.getMemberCount(groupId)
        return GroupResponse(
            id = group.id,
            name = group.name,
            createdBy = group.createdBy,
            createdByName = creatorUser?.username ?: "Unknown",
            memberCount = memberCount,
            createdAt = group.createdAt
        )
    }

    suspend fun addMember(groupId: String, requestingUserId: String, usernameOrEmail: String): String {
        ensureMember(groupId, requestingUserId)

        val targetUser = userRepository.findUserByUsername(usernameOrEmail)
            ?: userRepository.findUserByEmail(usernameOrEmail)
            ?: throw NotFoundException("Không tìm thấy người dùng '$usernameOrEmail'")

        val added = groupRepository.addMember(groupId, targetUser.id)
        if (!added) {
            throw com.splitbill.exceptions.ConflictException("'${targetUser.username}' đã là thành viên nhóm")
        }

        return "Đã thêm '${targetUser.username}' vào nhóm"
    }

    suspend fun joinGroup(groupId: String, requestingUserId: String): String {
        // Validate group exists
        val group = groupRepository.getGroupById(groupId)
            ?: throw NotFoundException("Không tìm thấy nhóm với mã ID này")
        
        val added = groupRepository.addMember(groupId, requestingUserId)
        if (!added) {
            throw com.splitbill.exceptions.ConflictException("Bạn đã là thành viên của nhóm này rồi")
        }

        return "Đã tham gia nhóm '${group.name}'"
    }

    suspend fun getMembers(groupId: String, userId: String): List<MemberResponse> {
        ensureMember(groupId, userId)

        val members = groupRepository.getMembers(groupId)
        return members.map {
            MemberResponse(
                userId = it.userId,
                username = it.username,
                email = it.email,
                joinedAt = it.joinedAt
            )
        }
    }

    /**
     * Helper kiểm tra quyền thành viên — dùng chung cho tất cả endpoint.
     * Throw ForbiddenException nếu user không phải thành viên.
     */
    suspend fun ensureMember(groupId: String, userId: String) {
        if (!groupRepository.isMember(groupId, userId)) {
            throw ForbiddenException("Bạn không phải thành viên nhóm này")
        }
    }
}
