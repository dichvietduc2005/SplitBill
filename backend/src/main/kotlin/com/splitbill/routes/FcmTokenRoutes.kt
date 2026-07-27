package com.splitbill.routes

import com.splitbill.models.MessageResponse
import com.splitbill.models.RegisterFcmTokenRequest
import com.splitbill.data.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.fcmTokenRoutes(userRepository: UserRepository) {
    route("/fcm") {
        post("/register") {
            val userId = call.currentUserId()
            val request = call.receive<RegisterFcmTokenRequest>()
            val success = userRepository.updateFcmToken(userId, request.token)
            if (success) {
                call.respond(HttpStatusCode.OK, MessageResponse("Đăng ký FCM token thành công"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, MessageResponse("Lỗi khi đăng ký FCM token"))
            }
        }

        delete("/unregister") {
            val userId = call.currentUserId()
            val success = userRepository.updateFcmToken(userId, null)
            if (success) {
                call.respond(HttpStatusCode.OK, MessageResponse("Hủy đăng ký FCM token thành công"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, MessageResponse("Lỗi khi hủy đăng ký FCM token"))
            }
        }
    }
}
