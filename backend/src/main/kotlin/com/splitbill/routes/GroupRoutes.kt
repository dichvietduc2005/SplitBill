package com.splitbill.routes

import com.splitbill.data.GroupRepository
import com.splitbill.data.UserRepository
import com.splitbill.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.groupRoutes() {
    route("/groups") {

        // POST /groups - Tạo nhóm mới
        post {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("id").asString()

            val request = call.receive<CreateGroupRequest>()
            if (request.name.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, MessageResponse("Tên nhóm không được để trống"))
                return@post
            }

            val group = GroupRepository.createGroup(request.name, userId)
            if (group != null) {
                val creatorUser = UserRepository.findUserById(userId)
                call.respond(
                    HttpStatusCode.Created,
                    GroupResponse(
                        id = group.id,
                        name = group.name,
                        createdBy = group.createdBy,
                        createdByName = creatorUser?.username ?: "Unknown",
                        memberCount = 1, // Vừa tạo nên chỉ có 1 thành viên (bản thân)
                        createdAt = group.createdAt
                    )
                )
            } else {
                call.respond(HttpStatusCode.InternalServerError, MessageResponse("Lỗi server khi tạo nhóm"))
            }
        }

        // GET /groups - Lấy danh sách nhóm của user đang đăng nhập
        get {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("id").asString()

            val groups = GroupRepository.getGroupsForUser(userId)
            val responses = groups.map { group ->
                val creatorUser = UserRepository.findUserById(group.createdBy)
                val memberCount = GroupRepository.getMemberCount(group.id)
                GroupResponse(
                    id = group.id,
                    name = group.name,
                    createdBy = group.createdBy,
                    createdByName = creatorUser?.username ?: "Unknown",
                    memberCount = memberCount,
                    createdAt = group.createdAt
                )
            }
            call.respond(HttpStatusCode.OK, responses)
        }

        // GET /groups/{id} - Lấy thông tin chi tiết nhóm
        get("/{id}") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("id").asString()
            val groupId = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest, MessageResponse("Thiếu ID nhóm")
            )

            // Kiểm tra quyền truy cập
            if (!GroupRepository.isMember(groupId, userId)) {
                call.respond(HttpStatusCode.Forbidden, MessageResponse("Bạn không phải thành viên nhóm này"))
                return@get
            }

            val group = GroupRepository.getGroupById(groupId)
            if (group == null) {
                call.respond(HttpStatusCode.NotFound, MessageResponse("Không tìm thấy nhóm"))
                return@get
            }

            val creatorUser = UserRepository.findUserById(group.createdBy)
            val memberCount = GroupRepository.getMemberCount(groupId)
            call.respond(
                HttpStatusCode.OK,
                GroupResponse(
                    id = group.id,
                    name = group.name,
                    createdBy = group.createdBy,
                    createdByName = creatorUser?.username ?: "Unknown",
                    memberCount = memberCount,
                    createdAt = group.createdAt
                )
            )
        }

        // POST /groups/{id}/members - Thêm thành viên vào nhóm
        post("/{id}/members") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("id").asString()
            val groupId = call.parameters["id"] ?: return@post call.respond(
                HttpStatusCode.BadRequest, MessageResponse("Thiếu ID nhóm")
            )

            // Kiểm tra quyền (chỉ thành viên mới có thể mời thêm)
            if (!GroupRepository.isMember(groupId, userId)) {
                call.respond(HttpStatusCode.Forbidden, MessageResponse("Bạn không phải thành viên nhóm này"))
                return@post
            }

            val request = call.receive<AddMemberRequest>()

            // Tìm user bằng username hoặc email
            val targetUser = UserRepository.findUserByUsername(request.usernameOrEmail)
                ?: UserRepository.findUserByEmail(request.usernameOrEmail)

            if (targetUser == null) {
                call.respond(HttpStatusCode.NotFound, MessageResponse("Không tìm thấy người dùng '${request.usernameOrEmail}'"))
                return@post
            }

            val added = GroupRepository.addMember(groupId, targetUser.id)
            if (added) {
                call.respond(HttpStatusCode.OK, MessageResponse("Đã thêm '${targetUser.username}' vào nhóm"))
            } else {
                call.respond(HttpStatusCode.Conflict, MessageResponse("'${targetUser.username}' đã là thành viên nhóm"))
            }
        }

        // GET /groups/{id}/members - Lấy danh sách thành viên nhóm
        get("/{id}/members") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("id").asString()
            val groupId = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest, MessageResponse("Thiếu ID nhóm")
            )

            if (!GroupRepository.isMember(groupId, userId)) {
                call.respond(HttpStatusCode.Forbidden, MessageResponse("Bạn không phải thành viên nhóm này"))
                return@get
            }

            val members = GroupRepository.getMembers(groupId)
            val responses = members.map {
                MemberResponse(
                    userId = it.userId,
                    username = it.username,
                    email = it.email,
                    joinedAt = it.joinedAt
                )
            }
            call.respond(HttpStatusCode.OK, responses)
        }
    }
}
