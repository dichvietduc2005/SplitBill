package com.splitbill.service

import com.splitbill.data.*
import com.splitbill.exceptions.ForbiddenException
import com.splitbill.exceptions.InternalException
import com.splitbill.exceptions.NotFoundException
import com.splitbill.exceptions.ValidationException
import com.splitbill.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    private val userRepository: UserRepository,
    private val settlementRepository: SettlementRepository,
    private val fcmService: FcmService,
    private val storageService: StorageService,
    private val activityRepository: ActivityRepository
) {

    private fun formatMoney(amount: Double, currency: String): String {
        val df = java.text.DecimalFormat("#,###")
        return "${df.format(amount)} $currency"
    }

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
            currency = request.currency,
            exchangeRate = request.exchangeRate,
            splits = splits
        ) ?: throw InternalException("Lỗi server khi tạo hóa đơn")

        // Gửi thông báo FCM và Ghi log hoạt động (bất đồng bộ để response trả về tức thì)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val group = groupRepository.getGroupById(request.groupId)
                val actor = userRepository.findUserById(userId)
                if (group != null && actor != null) {
                    val amountFormatted = formatMoney(request.totalAmount, request.currency)
                    fcmService.sendToGroup(
                        groupId = request.groupId,
                        excludeUserId = userId,
                        title = group.name,
                        body = "${actor.username} vừa thêm hóa đơn '${request.description}': $amountFormatted",
                        type = "BILL_CREATED"
                    )

                    activityRepository.createLog(
                        groupId = request.groupId,
                        userId = userId,
                        activityType = "BILL_CREATED",
                        description = "${actor.username} đã thêm hóa đơn '${request.description}': $amountFormatted"
                    )
                }
            } catch (e: Exception) {
                // Ignore background notification errors
            }
        }

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
     * Cập nhật thông tin hóa đơn — kiểm tra quyền thành viên và validate splits.
     */
    suspend fun updateBill(billId: String, request: UpdateBillRequest, userId: String): BillResponse {
        val existingBill = billRepository.getBillById(billId)
            ?: throw NotFoundException("Không tìm thấy hóa đơn")

        // Kiểm tra quyền: người thực hiện sửa phải là thành viên nhóm chứa bill
        if (!groupRepository.isMember(existingBill.groupId, userId)) {
            throw ForbiddenException("Bạn không có quyền sửa hóa đơn này")
        }

        // Kiểm tra người trả tiền cũng phải là thành viên nhóm
        if (!groupRepository.isMember(existingBill.groupId, request.paidByUserId)) {
            throw ValidationException("Người trả tiền không phải thành viên nhóm")
        }

        // Kiểm tra tất cả người trong splits đều là thành viên nhóm
        for (split in request.splits) {
            if (!groupRepository.isMember(existingBill.groupId, split.userId)) {
                throw ValidationException("User ${split.userId} không phải thành viên nhóm")
            }
        }

        val splits = request.splits.map { Pair(it.userId, it.amount) }
        val updatedBill = billRepository.updateBill(
            billId = billId,
            description = request.description,
            totalAmount = request.totalAmount,
            paidByUserId = request.paidByUserId,
            currency = request.currency,
            exchangeRate = request.exchangeRate,
            splits = splits
        ) ?: throw InternalException("Lỗi server khi cập nhật hóa đơn")

        // Gửi thông báo FCM và Ghi log hoạt động bất đồng bộ
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val group = groupRepository.getGroupById(existingBill.groupId)
                val actor = userRepository.findUserById(userId)
                if (group != null && actor != null) {
                    val amountFormatted = formatMoney(request.totalAmount, request.currency)
                    fcmService.sendToGroup(
                        groupId = existingBill.groupId,
                        excludeUserId = userId,
                        title = group.name,
                        body = "${actor.username} vừa sửa hóa đơn '${request.description}': $amountFormatted",
                        type = "BILL_EDITED"
                    )

                    activityRepository.createLog(
                        groupId = existingBill.groupId,
                        userId = userId,
                        activityType = "BILL_UPDATED",
                        description = "${actor.username} đã cập nhật hóa đơn '${request.description}': $amountFormatted"
                    )
                }
            } catch (e: Exception) {
                // Ignore background notification errors
            }
        }

        return toBillResponse(updatedBill)
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

        // Gửi thông báo FCM và Ghi log hoạt động bất đồng bộ
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val group = groupRepository.getGroupById(bill.groupId)
                val actor = userRepository.findUserById(userId)
                if (group != null && actor != null) {
                    fcmService.sendToGroup(
                        groupId = bill.groupId,
                        excludeUserId = userId,
                        title = group.name,
                        body = "${actor.username} vừa xóa hóa đơn '${bill.description}'",
                        type = "BILL_DELETED"
                    )

                    activityRepository.createLog(
                        groupId = bill.groupId,
                        userId = userId,
                        activityType = "BILL_DELETED",
                        description = "${actor.username} đã xóa hóa đơn '${bill.description}'"
                    )
                }
            } catch (e: Exception) {
                // Ignore background notification errors
            }
        }

        return "Đã xóa hóa đơn thành công"
    }

    suspend fun uploadReceipt(billId: String, fileBytes: ByteArray, userId: String): String {
        val bill = billRepository.getBillById(billId)
            ?: throw NotFoundException("Không tìm thấy hóa đơn")

        if (!groupRepository.isMember(bill.groupId, userId)) {
            throw ForbiddenException("Bạn không có quyền sửa hóa đơn này")
        }

        val receiptPath = storageService.saveReceipt(bill.groupId, billId, fileBytes)
        val finalUrl = if (receiptPath.startsWith("http") || receiptPath.startsWith("/")) receiptPath else "/$receiptPath"

        val success = billRepository.updateReceiptUrl(billId, finalUrl)
        if (!success) {
            throw InternalException("Lỗi khi lưu ảnh hóa đơn vào database")
        }

        return finalUrl
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
        val settlements = settlementRepository.getSettlementsForGroup(groupId)

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

        val simplifiedDebts = DebtSimplifier.simplify(bills, allSplits, settlements, memberMap)

        return DebtResponse(
            groupId = groupId,
            groupName = group.name,
            debts = simplifiedDebts,
            totalTransactions = simplifiedDebts.size
        )
    }

    suspend fun updateBillPaidStatus(billId: String, isPaid: Boolean, userId: String): Boolean {
        val bill = billRepository.getBillById(billId)
            ?: throw NotFoundException("Không tìm thấy hóa đơn")
        if (!groupRepository.isMember(bill.groupId, userId)) {
            throw ForbiddenException("Bạn không phải thành viên nhóm này")
        }
        val success = billRepository.updateBillPaidStatus(billId, isPaid)
        if (success) {
            val group = groupRepository.getGroupById(bill.groupId)
            val actor = userRepository.findUserById(userId)
            if (group != null && actor != null) {
                fcmService.sendToGroup(
                    groupId = bill.groupId,
                    excludeUserId = userId,
                    title = group.name,
                    body = "Hóa đơn '${bill.description}' đã được đánh dấu là ${if (isPaid) "đã thanh toán" else "chưa thanh toán"}",
                    type = "BILL_UPDATED"
                )

                activityRepository.createLog(
                    groupId = bill.groupId,
                    userId = userId,
                    activityType = "BILL_UPDATED",
                    description = "${actor.username} đã đánh dấu hóa đơn '${bill.description}' là ${if (isPaid) "đã thanh toán" else "chưa thanh toán"}"
                )
            }
        }
        return success
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
            currency = bill.currency,
            exchangeRate = bill.exchangeRate.toDouble(),
            receiptUrl = bill.receiptUrl,
            isPaid = bill.isPaid,
            splits = splitResponses,
            createdAt = bill.createdAt
        )
    }
}
