package com.splitbill.routes

import com.splitbill.exceptions.ValidationException
import com.splitbill.models.*
import com.splitbill.service.BillService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Bill Routes — gọn gàng, chỉ nhận request → gọi service → trả response.
 * Logic nghiệp vụ nằm trong BillService.
 * Hỗ trợ phân trang (limit/offset) cho danh sách hóa đơn.
 */
fun Route.billRoutes(billService: BillService) {

    route("/bills") {

        // POST /bills - Tạo hóa đơn mới
        post {
            val userId = call.currentUserId()
            val request = call.receive<CreateBillRequest>()
            val response = billService.createBill(request, userId)
            call.respond(HttpStatusCode.Created, response)
        }

        // GET /bills?groupId={groupId}&limit={limit}&offset={offset} - Lấy danh sách hóa đơn (có phân trang)
        get {
            val userId = call.currentUserId()

            val groupId = call.request.queryParameters["groupId"]
                ?: throw ValidationException("Thiếu groupId")

            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0

            val response = billService.getBillsForGroup(groupId, userId, limit, offset)
            call.respond(HttpStatusCode.OK, response)
        }

        // DELETE /bills/{id} - Xóa hóa đơn
        delete("/{id}") {
            val userId = call.currentUserId()
            val billId = call.parameters["id"]
                ?: throw ValidationException("Thiếu ID hóa đơn")
            val message = billService.deleteBill(billId, userId)
            call.respond(HttpStatusCode.OK, MessageResponse(message))
        }
    }

    // ==========================================
    // API THUẬT TOÁN TỐI GIẢN NỢ
    // ==========================================

    route("/debts") {
        // GET /debts/{groupId} - Tính toán và trả về danh sách nợ tối giản
        get("/{groupId}") {
            val userId = call.currentUserId()
            val groupId = call.parameters["groupId"]
                ?: throw ValidationException("Thiếu groupId")
            val response = billService.getSimplifiedDebts(groupId, userId)
            call.respond(HttpStatusCode.OK, response)
        }
    }
}
