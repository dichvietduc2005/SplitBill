package com.splitbill.routes

import com.splitbill.exceptions.ValidationException
import com.splitbill.models.*
import com.splitbill.service.BillService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.content.*
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

        // PUT /bills/{id} - Sửa hóa đơn
        put("/{id}") {
            val userId = call.currentUserId()
            val billId = call.parameters["id"]
                ?: throw ValidationException("Thiếu ID hóa đơn")
            val request = call.receive<UpdateBillRequest>()
            val response = billService.updateBill(billId, request, userId)
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

        // POST /bills/{id}/receipt - Upload ảnh hóa đơn
        post("/{id}/receipt") {
            val userId = call.currentUserId()
            val billId = call.parameters["id"]
                ?: throw ValidationException("Thiếu ID hóa đơn")

            val multipart = call.receiveMultipart()
            var fileBytes: ByteArray? = null
            multipart.forEachPart { part ->
                if (part is io.ktor.http.content.PartData.FileItem) {
                    fileBytes = part.streamProvider().readBytes()
                }
                part.dispose()
            }

            if (fileBytes == null) {
                throw ValidationException("Không nhận được file ảnh")
            }

            val receiptUrl = billService.uploadReceipt(billId, fileBytes!!, userId)
            call.respond(HttpStatusCode.OK, mapOf("receiptUrl" to receiptUrl))
        }

        // POST /bills/{id}/pay - Đánh dấu hóa đơn đã thanh toán
        post("/{id}/pay") {
            val userId = call.currentUserId()
            val billId = call.parameters["id"]
                ?: throw ValidationException("Thiếu ID hóa đơn")
            val isPaid = call.request.queryParameters["isPaid"]?.toBooleanStrictOrNull() ?: true
            val success = billService.updateBillPaidStatus(billId, isPaid, userId)
            if (success) {
                call.respond(HttpStatusCode.OK, MessageResponse("Cập nhật trạng thái hóa đơn thành công"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, MessageResponse("Lỗi khi cập nhật trạng thái hóa đơn"))
            }
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
