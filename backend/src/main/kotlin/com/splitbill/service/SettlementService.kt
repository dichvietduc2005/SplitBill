package com.splitbill.service

import com.splitbill.data.*
import com.splitbill.exceptions.ForbiddenException
import com.splitbill.exceptions.InternalException
import com.splitbill.exceptions.NotFoundException
import com.splitbill.exceptions.ValidationException
import com.splitbill.models.*

class SettlementService(
    private val settlementRepository: SettlementRepository,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val fcmService: FcmService,
    private val activityRepository: com.splitbill.data.ActivityRepository
) {

    private fun formatMoney(amount: Double): String {
        val df = java.text.DecimalFormat("#,###")
        return "${df.format(amount)} VND"
    }

    suspend fun createSettlement(request: CreateSettlementRequest, callerUserId: String): SettlementResponse {
        val fromUserId = request.fromUserId ?: callerUserId
        val toUserId = request.toUserId

        if (callerUserId != fromUserId && callerUserId != toUserId) {
            throw ForbiddenException("Bạn không có quyền ghi nhận giao dịch này")
        }

        // Kiểm tra quyền: người thanh toán phải ở trong nhóm
        if (!groupRepository.isMember(request.groupId, fromUserId)) {
            throw ForbiddenException("Người thanh toán không phải thành viên nhóm")
        }

        // Kiểm tra người nhận tiền cũng phải ở trong nhóm
        if (!groupRepository.isMember(request.groupId, toUserId)) {
            throw ValidationException("Người nhận tiền không phải thành viên nhóm")
        }

        if (request.amount <= 0) {
            throw ValidationException("Số tiền thanh toán phải lớn hơn 0")
        }

        val settlement = settlementRepository.createSettlement(
            groupId = request.groupId,
            fromUserId = fromUserId,
            toUserId = toUserId,
            amount = request.amount,
            note = request.note
        ) ?: throw InternalException("Lỗi server khi lưu thanh toán")

        // Gửi thông báo FCM và Ghi log hoạt động
        val group = groupRepository.getGroupById(request.groupId)
        val fromUser = userRepository.findUserById(fromUserId)
        val toUser = userRepository.findUserById(toUserId)
        if (group != null && fromUser != null && toUser != null) {
            val amountFormatted = formatMoney(request.amount)
            fcmService.sendToGroup(
                groupId = request.groupId,
                excludeUserId = callerUserId,
                title = group.name,
                body = "${fromUser.username} đã thanh toán $amountFormatted cho ${toUser.username}",
                type = "DEBT_SETTLED"
            )

            activityRepository.createLog(
                groupId = request.groupId,
                userId = callerUserId,
                activityType = "DEBT_SETTLED",
                description = "${fromUser.username} đã thanh toán $amountFormatted cho ${toUser.username}"
            )
        }

        return toSettlementResponse(settlement)
    }

    suspend fun getSettlementsForGroup(groupId: String, userId: String): List<SettlementResponse> {
        if (!groupRepository.isMember(groupId, userId)) {
            throw ForbiddenException("Bạn không phải thành viên nhóm này")
        }

        val settlements = settlementRepository.getSettlementsForGroup(groupId)
        return settlements.map { toSettlementResponse(it) }
    }

    suspend fun deleteSettlement(settlementId: String, userId: String): String {
        val settlement = settlementRepository.getSettlementById(settlementId)
            ?: throw NotFoundException("Không tìm thấy giao dịch thanh toán")

        if (!groupRepository.isMember(settlement.groupId, userId)) {
            throw ForbiddenException("Bạn không có quyền xóa giao dịch này")
        }

        val deleted = settlementRepository.deleteSettlement(settlementId)
        if (!deleted) {
            throw InternalException("Lỗi server khi xóa giao dịch")
        }

        return "Đã xóa giao dịch thanh toán thành công"
    }

    private suspend fun toSettlementResponse(settlement: Settlement): SettlementResponse {
        val fromUser = userRepository.findUserById(settlement.fromUserId)
        val toUser = userRepository.findUserById(settlement.toUserId)

        return SettlementResponse(
            id = settlement.id,
            groupId = settlement.groupId,
            fromUserId = settlement.fromUserId,
            fromUsername = fromUser?.username ?: "Unknown",
            toUserId = settlement.toUserId,
            toUsername = toUser?.username ?: "Unknown",
            amount = settlement.amount.toDouble(),
            note = settlement.note,
            createdAt = settlement.createdAt
        )
    }
}
