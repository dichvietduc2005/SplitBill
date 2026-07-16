package com.splitbill.routes

import com.splitbill.exceptions.ValidationException
import com.splitbill.models.CreateInviteRequest
import com.splitbill.models.JoinGroupWithCodeRequest
import com.splitbill.models.MessageResponse
import com.splitbill.service.InviteService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.inviteRoutes(inviteService: InviteService) {

    route("/invites") {

        // POST /invites/join - Tham gia nhóm bằng mã mời
        post("/join") {
            val userId = call.currentUserId()
            val request = call.receive<JoinGroupWithCodeRequest>()
            val response = inviteService.joinByInvite(request.inviteCode, userId)
            call.respond(HttpStatusCode.OK, response)
        }

        // DELETE /invites/{id} - Xóa/Hủy mã mời
        delete("/{id}") {
            val userId = call.currentUserId()
            val inviteId = call.parameters["id"]
                ?: throw ValidationException("Thiếu ID mã mời")
            val message = inviteService.deleteInvite(inviteId, userId)
            call.respond(HttpStatusCode.OK, MessageResponse(message))
        }
    }

    route("/groups/{groupId}/invites") {

        // POST /groups/{groupId}/invites - Tạo mã mời mới cho nhóm
        post {
            val userId = call.currentUserId()
            val groupId = call.parameters["groupId"]
                ?: throw ValidationException("Thiếu groupId")
            val request = try { call.receive<CreateInviteRequest>() } catch (e: Exception) { CreateInviteRequest() }
            val response = inviteService.createInvite(groupId, userId, request.maxUses)
            call.respond(HttpStatusCode.Created, response)
        }

        // GET /groups/{groupId}/invites - Lấy danh sách mã mời đang kích hoạt của nhóm
        get {
            val userId = call.currentUserId()
            val groupId = call.parameters["groupId"]
                ?: throw ValidationException("Thiếu groupId")
            val response = inviteService.getActiveInvites(groupId, userId)
            call.respond(HttpStatusCode.OK, response)
        }
    }
}
