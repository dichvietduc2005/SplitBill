package com.splitbill.routes

import com.splitbill.exceptions.ValidationException
import com.splitbill.models.CreateSettlementRequest
import com.splitbill.models.MessageResponse
import com.splitbill.service.SettlementService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.settlementRoutes(settlementService: SettlementService) {

    route("/settlements") {

        // POST /settlements - Ghi nhận thanh toán trả nợ
        post {
            val userId = call.currentUserId()
            val request = call.receive<CreateSettlementRequest>()
            val response = settlementService.createSettlement(request, userId)
            call.respond(HttpStatusCode.Created, response)
        }

        // GET /settlements/{groupId} - Lấy lịch sử thanh toán nợ của một nhóm
        get("/{groupId}") {
            val userId = call.currentUserId()
            val groupId = call.parameters["groupId"]
                ?: throw ValidationException("Thiếu groupId")
            val response = settlementService.getSettlementsForGroup(groupId, userId)
            call.respond(HttpStatusCode.OK, response)
        }

        // DELETE /settlements/{id} - Hủy bỏ một giao dịch thanh toán
        delete("/{id}") {
            val userId = call.currentUserId()
            val settlementId = call.parameters["id"]
                ?: throw ValidationException("Thiếu ID giao dịch")
            val message = settlementService.deleteSettlement(settlementId, userId)
            call.respond(HttpStatusCode.OK, MessageResponse(message))
        }
    }
}
