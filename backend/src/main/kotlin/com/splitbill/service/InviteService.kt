package com.splitbill.service

import com.splitbill.data.*
import com.splitbill.exceptions.ForbiddenException
import com.splitbill.exceptions.InternalException
import com.splitbill.exceptions.NotFoundException
import com.splitbill.exceptions.ValidationException
import com.splitbill.models.InviteResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class InviteService(
    private val inviteRepository: InviteRepository,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository
) {

    suspend fun createInvite(groupId: String, userId: String, maxUses: Int? = null): InviteResponse {
        // Kiểm tra quyền: Người tạo mã mời phải là thành viên nhóm
        if (!groupRepository.isMember(groupId, userId)) {
            throw ForbiddenException("Bạn không phải thành viên nhóm này")
        }

        val group = groupRepository.getGroupById(groupId)
            ?: throw NotFoundException("Không tìm thấy nhóm")

        // Tạo invite code unique
        var inviteCode = generateInviteCode()
        while (inviteRepository.findByCode(inviteCode) != null) {
            inviteCode = generateInviteCode()
        }

        val expiresAt = LocalDateTime.now().plusDays(7)
        val invite = inviteRepository.createInvite(
            groupId = groupId,
            inviteCode = inviteCode,
            createdBy = userId,
            expiresAt = expiresAt,
            maxUses = maxUses
        ) ?: throw InternalException("Lỗi server khi tạo mã mời")

        return toInviteResponse(invite, group.name)
    }

    suspend fun getActiveInvites(groupId: String, userId: String): List<InviteResponse> {
        if (!groupRepository.isMember(groupId, userId)) {
            throw ForbiddenException("Bạn không phải thành viên nhóm này")
        }

        val group = groupRepository.getGroupById(groupId)
            ?: throw NotFoundException("Không tìm thấy nhóm")

        val invites = inviteRepository.getActiveInvites(groupId)
        return invites.map { toInviteResponse(it, group.name) }
    }

    suspend fun joinByInvite(inviteCode: String, userId: String): InviteResponse {
        val invite = inviteRepository.findByCode(inviteCode)
            ?: throw NotFoundException("Mã mời không tồn tại hoặc đã hết hiệu lực")

        val group = groupRepository.getGroupById(invite.groupId)
            ?: throw NotFoundException("Không tìm thấy nhóm tương ứng với mã mời")

        // Kiểm tra hạn sử dụng
        val expiresAtTime = LocalDateTime.parse(invite.expiresAt.substring(0, 19))
        if (expiresAtTime.isBefore(LocalDateTime.now())) {
            throw ValidationException("Mã mời đã hết hạn sử dụng (giới hạn 7 ngày)")
        }

        // Kiểm tra số lượt dùng tối đa
        if (invite.maxUses != null && invite.useCount >= invite.maxUses) {
            throw ValidationException("Mã mời đã đạt tối đa số lượt sử dụng")
        }

        // Nếu đã là thành viên thì không cần thêm, chỉ trả về thành công
        if (groupRepository.isMember(invite.groupId, userId)) {
            return toInviteResponse(invite, group.name)
        }

        // Thêm thành viên
        val added = groupRepository.addMember(invite.groupId, userId)
        if (!added) {
            throw InternalException("Lỗi khi tham gia nhóm")
        }

        // Tăng lượt dùng
        inviteRepository.incrementUseCount(invite.id)

        // Trích xuất lại để lấy useCount mới nhất
        val updatedInvite = inviteRepository.findByCode(inviteCode) ?: invite

        return toInviteResponse(updatedInvite, group.name)
    }

    suspend fun deleteInvite(inviteId: String, userId: String): String {
        val invite = inviteRepository.getInviteById(inviteId)
            ?: throw NotFoundException("Mã mời không tồn tại")

        if (!groupRepository.isMember(invite.groupId, userId)) {
            throw ForbiddenException("Bạn không có quyền xóa mã mời của nhóm này")
        }

        val deleted = inviteRepository.deleteInvite(inviteId)
        if (!deleted) {
            throw InternalException("Lỗi server khi xóa mã mời")
        }

        return "Đã xóa mã mời thành công"
    }

    private fun generateInviteCode(): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..8)
            .map { allowedChars.random() }
            .joinToString("")
    }

    private fun toInviteResponse(invite: GroupInvite, groupName: String): InviteResponse {
        return InviteResponse(
            id = invite.id,
            groupId = invite.groupId,
            groupName = groupName,
            inviteCode = invite.inviteCode,
            inviteUrl = "splitbill://join/${invite.inviteCode}",
            expiresAt = invite.expiresAt,
            maxUses = invite.maxUses,
            useCount = invite.useCount,
            createdAt = invite.createdAt
        )
    }
}
