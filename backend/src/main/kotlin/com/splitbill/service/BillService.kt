package com.splitbill.service

import com.splitbill.data.*
import com.splitbill.exceptions.ForbiddenException
import com.splitbill.exceptions.InternalException
import com.splitbill.exceptions.NotFoundException
import com.splitbill.exceptions.ValidationException
import com.splitbill.models.*

/**
 * BillService — chứa logic nghiệp vụ quản lý hóa đơn & tối giản nợ:
 * - Tạo hóa đơn (kiểm tra quyền, validate thành viên)
 * - Lấy danh sách hóa đơn (có phân trang)
 * - Xóa hóa đơn
 * - Tính toán nợ tối giản (Debt Simplification)
 */
class BillService(
    private val billRepository: BillRepository,
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository
) {

    /**
     * Tạo hóa đơn mới — kiểm tra quyền thành viên cho creator, payer, và tất cả người trong splits.
     */
    suspend fun createBill(request: CreateBillRequest, userId: String): BillResponse {
        // Kiểm tra quyền: người tạo bill phải là thành viên nhóm
        if (!groupRepository.isMember(request.groupId, userId)) {
            throw ForbiddenException("Bạn không phải thành viên nhóm này")
        }

        // Kiểm tra người trả tiền cũng phải là thành viên nhóm
        if (!groupRepository.isMember(request.groupId, request.paidByUserId)) {
            throw ValidationException("Người trả tiền không phải thành viên nhóm")
        }

        // Kiểm tra tất cả người trong splits đều là thành viên
        for (split in request.splits) {
            if (!groupRepository.isMember(request.groupId, split.userId)) {
                throw ValidationException("User ${split.userId} không phải thành viên nhóm")
            }
        }

        val splits = request.splits.map { Pair(it.userId, it.amount) }
        val bill = billRepository.createBill(
            groupId = request.groupId,
            description = request.description,
            totalAmount = request.totalAmount,
            paidByUserId = request.paidByUserId,
            splits = splits
        ) ?: throw InternalException("Lỗi server khi tạo hóa đơn")

        return toBillResponse(bill)
    }

    /**
     * Lấy danh sách hóa đơn của nhóm — có phân trang.
     */
    suspend fun getBillsForGroup(groupId: String, userId: String, limit: Int = 50, offset: Int = 0): PaginatedBillResponse {
        if (!groupRepository.isMember(groupId, userId)) {
            throw ForbiddenException("Bạn không phải thành viên nhóm này")
        }

        val bills = billRepository.getBillsForGroup(groupId, limit, offset)
        val total = billRepository.countBillsForGroup(groupId)

        val responses = bills.map { toBillResponse(it) }
        return PaginatedBillResponse(
            data = responses,
            total = total,
            limit = limit,
            offset = offset
        )
    }

    /**
     * Xóa hóa đơn — kiểm tra quyền thành viên nhóm.
     */
    suspend fun deleteBill(billId: String, userId: String): String {
        val bill = billRepository.getBillById(billId)
            ?: throw NotFoundException("Không tìm thấy hóa đơn")

        if (!groupRepository.isMember(bill.groupId, userId)) {
            throw ForbiddenException("Bạn không có quyền xóa hóa đơn này")
        }

        val deleted = billRepository.deleteBill(billId)
        if (!deleted) {
            throw InternalException("Lỗi khi xóa hóa đơn")
        }

        return "Đã xóa hóa đơn thành công"
    }

    /**
     * Tính toán nợ tối giản cho nhóm — chạy thuật toán Greedy Max-Heap.
     */
    suspend fun getSimplifiedDebts(groupId: String, userId: String): DebtResponse {
        if (!groupRepository.isMember(groupId, userId)) {
            throw ForbiddenException("Bạn không phải thành viên nhóm này")
        }

        val group = groupRepository.getGroupById(groupId)
            ?: throw NotFoundException("Không tìm thấy nhóm")

        val bills = billRepository.getAllBillsForGroup(groupId)
        val allSplits = billRepository.getAllSplitsForGroup(groupId)

        if (bills.isEmpty()) {
            return DebtResponse(
                groupId = groupId,
                groupName = group.name,
                debts = emptyList(),
                totalTransactions = 0
            )
        }

        val members = groupRepository.getMembers(groupId)
        val memberMap = members.associate { it.userId to it.username }

        val simplifiedDebts = DebtSimplifier.simplify(bills, allSplits, memberMap)

        return DebtResponse(
            groupId = groupId,
            groupName = group.name,
            debts = simplifiedDebts,
            totalTransactions = simplifiedDebts.size
        )
    }

    /**
     * Helper chuyển đổi Bill entity → BillResponse DTO (kèm username và splits).
     */
    private suspend fun toBillResponse(bill: Bill): BillResponse {
        val paidByUser = userRepository.findUserById(bill.paidByUserId)
        val billSplits = billRepository.getSplitsForBill(bill.id)
        val splitResponses = billSplits.map { s ->
            val user = userRepository.findUserById(s.userId)
            BillSplitResponse(
                userId = s.userId,
                username = user?.username ?: "Unknown",
                amountOwed = s.amountOwed.toDouble()
            )
        }

        return BillResponse(
            id = bill.id,
            groupId = bill.groupId,
            description = bill.description,
            totalAmount = bill.totalAmount.toDouble(),
            paidByUserId = bill.paidByUserId,
            paidByUsername = paidByUser?.username ?: "Unknown",
            splits = splitResponses,
            createdAt = bill.createdAt
        )
    }
}
