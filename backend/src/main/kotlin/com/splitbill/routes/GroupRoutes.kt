package com.splitbill.routes

import com.splitbill.exceptions.ValidationException
import com.splitbill.models.*
import com.splitbill.service.GroupService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Group Routes — gọn gàng, chỉ nhận request → gọi service → trả response.
 * Logic nghiệp vụ nằm trong GroupService.
 */
fun Route.groupRoutes() {
    val groupService by inject<GroupService>()

    route("/groups") {

        // POST /groups - Tạo nhóm mới
        post {
            val userId = call.currentUserId()
            val request = call.receive<CreateGroupRequest>()
            val response = groupService.createGroup(request.name, userId)
            call.respond(HttpStatusCode.Created, response)
        }

        // GET /groups - Lấy danh sách nhóm của user đang đăng nhập
        get {
            val userId = call.currentUserId()
            val responses = groupService.getGroupsForUser(userId)
            call.respond(HttpStatusCode.OK, responses)
        }

        // GET /groups/{id} - Lấy thông tin chi tiết nhóm
        get("/{id}") {
            val userId = call.currentUserId()
            val groupId = call.parameters["id"]
                ?: throw ValidationException("Thiếu ID nhóm")
            val response = groupService.getGroupDetail(groupId, userId)
            call.respond(HttpStatusCode.OK, response)
        }

        // POST /groups/{id}/members - Thêm thành viên vào nhóm
        post("/{id}/members") {
            val userId = call.currentUserId()
            val groupId = call.parameters["id"]
                ?: throw ValidationException("Thiếu ID nhóm")
            val request = call.receive<AddMemberRequest>()
            val message = groupService.addMember(groupId, userId, request.usernameOrEmail)
            call.respond(HttpStatusCode.OK, MessageResponse(message))
        }

        // GET /groups/{id}/members - Lấy danh sách thành viên nhóm
        get("/{id}/members") {
            val userId = call.currentUserId()
            val groupId = call.parameters["id"]
                ?: throw ValidationException("Thiếu ID nhóm")
            val responses = groupService.getMembers(groupId, userId)
            call.respond(HttpStatusCode.OK, responses)
        }
    }
}

/**
 * Extension function tiện lợi — lấy userId từ JWT Principal.
 * Dùng chung cho tất cả Route cần xác thực.
 */
fun ApplicationCall.currentUserId(): String {
    val principal = principal<JWTPrincipal>()
    return principal!!.payload.getClaim("id").asString()
}
