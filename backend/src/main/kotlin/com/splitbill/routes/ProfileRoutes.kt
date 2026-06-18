package com.splitbill.routes

import com.splitbill.exceptions.ValidationException
import com.splitbill.models.MessageResponse
import com.splitbill.models.UpdateBankInfoRequest
import com.splitbill.service.ProfileService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Profile Routes — gọn gàng, chỉ nhận request → gọi service → trả response.
 * Logic nghiệp vụ nằm trong ProfileService.
 */
fun Route.profileRoutes(profileService: ProfileService) {

    route("/profile") {

        // GET /profile - Lấy thông tin profile của user đang đăng nhập
        get {
            val userId = call.currentUserId()
            val response = profileService.getMyProfile(userId)
            call.respond(HttpStatusCode.OK, response)
        }

        // GET /profile/{userId} - Lấy thông tin bank của user cụ thể (cho VietQR)
        get("/{userId}") {
            val targetUserId = call.parameters["userId"]
                ?: throw ValidationException("Thiếu userId")
            val response = profileService.getUserProfile(targetUserId)
            call.respond(HttpStatusCode.OK, response)
        }

        // PUT /profile/bank - Cập nhật thông tin ngân hàng
        put("/bank") {
            val userId = call.currentUserId()
            val request = call.receive<UpdateBankInfoRequest>()
            val message = profileService.updateBankInfo(userId, request)
            call.respond(HttpStatusCode.OK, MessageResponse(message))
        }
    }
}
