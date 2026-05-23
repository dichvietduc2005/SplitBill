package com.splitbill.routes

import com.splitbill.data.UserRepository
import com.splitbill.models.MessageResponse
import com.splitbill.models.ProfileResponse
import com.splitbill.models.UpdateBankInfoRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.profileRoutes() {
    route("/profile") {

        // GET /profile - Lấy thông tin profile của user đang đăng nhập
        get {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("id").asString()

            val user = UserRepository.findUserById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, MessageResponse("Không tìm thấy người dùng"))
                return@get
            }

            call.respond(
                HttpStatusCode.OK,
                ProfileResponse(
                    id = user.id,
                    username = user.username,
                    email = user.email,
                    avatarUrl = user.avatarUrl,
                    bankCode = user.bankCode,
                    accountNumber = user.accountNumber,
                    accountName = user.accountName
                )
            )
        }

        // GET /profile/{userId} - Lấy thông tin bank của một user cụ thể (để tạo QR thanh toán)
        get("/{userId}") {
            val requestingUserId = call.principal<JWTPrincipal>()!!.payload.getClaim("id").asString()
            val targetUserId = call.parameters["userId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest, MessageResponse("Thiếu userId")
            )

            val user = UserRepository.findUserById(targetUserId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, MessageResponse("Không tìm thấy người dùng"))
                return@get
            }

            // Trả về thông tin profile (chỉ bao gồm thông tin public + bank info để tạo QR)
            call.respond(
                HttpStatusCode.OK,
                ProfileResponse(
                    id = user.id,
                    username = user.username,
                    email = user.email,
                    avatarUrl = user.avatarUrl,
                    bankCode = user.bankCode,
                    accountNumber = user.accountNumber,
                    accountName = user.accountName
                )
            )
        }

        // PUT /profile/bank - Cập nhật thông tin ngân hàng
        put("/bank") {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal!!.payload.getClaim("id").asString()

            val request = call.receive<UpdateBankInfoRequest>()

            // Validate
            if (request.bankCode.isBlank() || request.accountNumber.isBlank() || request.accountName.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, MessageResponse("Vui lòng điền đầy đủ thông tin ngân hàng"))
                return@put
            }

            val success = UserRepository.updateBankInfo(
                userId = userId,
                bankCode = request.bankCode.uppercase().trim(),
                accountNumber = request.accountNumber.trim(),
                accountName = request.accountName.trim().uppercase()
            )

            if (success) {
                call.respond(HttpStatusCode.OK, MessageResponse("Cập nhật thông tin ngân hàng thành công"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, MessageResponse("Lỗi khi cập nhật thông tin"))
            }
        }
    }
}
