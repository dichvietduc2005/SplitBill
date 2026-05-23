package com.splitbill.routes

import com.splitbill.data.*
import com.splitbill.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.billRoutes() {
    route("/bills") {

        // POST /bills - Tạo hóa đơn mới
        post {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("id").asString()

            val request = call.receive<CreateBillRequest>()

            // Validate input
            if (request.description.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, MessageResponse("Mô tả hóa đơn không được để trống"))
                return@post
            }
            if (request.totalAmount <= 0) {
                call.respond(HttpStatusCode.BadRequest, MessageResponse("Tổng tiền phải lớn hơn 0"))
                return@post
            }
            if (request.splits.isEmpty()) {
                call.respond(HttpStatusCode.BadRequest, MessageResponse("Phải có ít nhất 1 người chia nợ"))
                return@post
            }

            // Kiểm tra tổng splits có khớp totalAmount không
            val splitsTotal = request.splits.sumOf { it.amount }
            if (kotlin.math.abs(splitsTotal - request.totalAmount) > 0.01) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    MessageResponse("Tổng các khoản chia ($splitsTotal) không khớp tổng hóa đơn (${request.totalAmount})")
                )
                return@post
            }

            // Kiểm tra quyền: người tạo bill phải là thành viên nhóm
            if (!GroupRepository.isMember(request.groupId, userId)) {
                call.respond(HttpStatusCode.Forbidden, MessageResponse("Bạn không phải thành viên nhóm này"))
                return@post
            }

            // Kiểm tra người trả tiền cũng phải là thành viên nhóm
            if (!GroupRepository.isMember(request.groupId, request.paidByUserId)) {
                call.respond(HttpStatusCode.BadRequest, MessageResponse("Người trả tiền không phải thành viên nhóm"))
                return@post
            }

            // Kiểm tra tất cả người trong splits đều là thành viên
            for (split in request.splits) {
                if (!GroupRepository.isMember(request.groupId, split.userId)) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        MessageResponse("User ${split.userId} không phải thành viên nhóm")
                    )
                    return@post
                }
            }

            val splits = request.splits.map { Pair(it.userId, it.amount) }
            val bill = BillRepository.createBill(
                groupId = request.groupId,
                description = request.description,
                totalAmount = request.totalAmount,
                paidByUserId = request.paidByUserId,
                splits = splits
            )

            if (bill != null) {
                val paidByUser = UserRepository.findUserById(bill.paidByUserId)
                val billSplits = BillRepository.getSplitsForBill(bill.id)
                val splitResponses = billSplits.map { s ->
                    val user = UserRepository.findUserById(s.userId)
                    BillSplitResponse(
                        userId = s.userId,
                        username = user?.username ?: "Unknown",
                        amountOwed = s.amountOwed.toDouble()
                    )
                }

                call.respond(
                    HttpStatusCode.Created,
                    BillResponse(
                        id = bill.id,
                        groupId = bill.groupId,
                        description = bill.description,
                        totalAmount = bill.totalAmount.toDouble(),
                        paidByUserId = bill.paidByUserId,
                        paidByUsername = paidByUser?.username ?: "Unknown",
                        splits = splitResponses,
                        createdAt = bill.createdAt
                    )
                )
            } else {
                call.respond(HttpStatusCode.InternalServerError, MessageResponse("Lỗi server khi tạo hóa đơn"))
            }
        }

        // GET /bills?groupId={groupId} - Lấy danh sách hóa đơn của nhóm
        get {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("id").asString()

            val groupId = call.request.queryParameters["groupId"]
            if (groupId.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, MessageResponse("Thiếu groupId"))
                return@get
            }

            if (!GroupRepository.isMember(groupId, userId)) {
                call.respond(HttpStatusCode.Forbidden, MessageResponse("Bạn không phải thành viên nhóm này"))
                return@get
            }

            val bills = BillRepository.getBillsForGroup(groupId)
            val responses = bills.map { bill ->
                val paidByUser = UserRepository.findUserById(bill.paidByUserId)
                val splits = BillRepository.getSplitsForBill(bill.id)
                val splitResponses = splits.map { s ->
                    val user = UserRepository.findUserById(s.userId)
                    BillSplitResponse(
                        userId = s.userId,
                        username = user?.username ?: "Unknown",
                        amountOwed = s.amountOwed.toDouble()
                    )
                }

                BillResponse(
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
            call.respond(HttpStatusCode.OK, responses)
        }

        // DELETE /bills/{id} - Xóa hóa đơn
        delete("/{id}") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("id").asString()
            val billId = call.parameters["id"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest, MessageResponse("Thiếu ID hóa đơn")
            )

            val bill = BillRepository.getBillById(billId)
            if (bill == null) {
                call.respond(HttpStatusCode.NotFound, MessageResponse("Không tìm thấy hóa đơn"))
                return@delete
            }

            // Chỉ thành viên nhóm mới được xóa
            if (!GroupRepository.isMember(bill.groupId, userId)) {
                call.respond(HttpStatusCode.Forbidden, MessageResponse("Bạn không có quyền xóa hóa đơn này"))
                return@delete
            }

            val deleted = BillRepository.deleteBill(billId)
            if (deleted) {
                call.respond(HttpStatusCode.OK, MessageResponse("Đã xóa hóa đơn thành công"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, MessageResponse("Lỗi khi xóa hóa đơn"))
            }
        }
    }

    // ==========================================
    // API THUẬT TOÁN TỐI GIẢN NỢ
    // ==========================================

    // GET /debts/{groupId} - Tính toán và trả về danh sách nợ tối giản
    route("/debts") {
        get("/{groupId}") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("id").asString()
            val groupId = call.parameters["groupId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest, MessageResponse("Thiếu groupId")
            )

            if (!GroupRepository.isMember(groupId, userId)) {
                call.respond(HttpStatusCode.Forbidden, MessageResponse("Bạn không phải thành viên nhóm này"))
                return@get
            }

            val group = GroupRepository.getGroupById(groupId)
            if (group == null) {
                call.respond(HttpStatusCode.NotFound, MessageResponse("Không tìm thấy nhóm"))
                return@get
            }

            // Lấy toàn bộ dữ liệu bills và splits
            val bills = BillRepository.getAllBillsForGroup(groupId)
            val allSplits = BillRepository.getAllSplitsForGroup(groupId)

            if (bills.isEmpty()) {
                call.respond(
                    HttpStatusCode.OK,
                    DebtResponse(
                        groupId = groupId,
                        groupName = group.name,
                        debts = emptyList(),
                        totalTransactions = 0
                    )
                )
                return@get
            }

            // Tạo map userId -> username
            val members = GroupRepository.getMembers(groupId)
            val memberMap = members.associate { it.userId to it.username }

            // Chạy thuật toán tối giản nợ
            val simplifiedDebts = DebtSimplifier.simplify(bills, allSplits, memberMap)

            call.respond(
                HttpStatusCode.OK,
                DebtResponse(
                    groupId = groupId,
                    groupName = group.name,
                    debts = simplifiedDebts,
                    totalTransactions = simplifiedDebts.size
                )
            )
        }
    }
}
