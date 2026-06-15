package com.splitbill.routes

import com.splitbill.models.LoginRequest
import com.splitbill.models.RegisterRequest
import com.splitbill.service.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/**
 * Auth Routes — gọn gàng, chỉ nhận request → gọi service → trả response.
 * Logic nghiệp vụ nằm trong AuthService.
 * Rate Limiting được áp dụng cho cả register và login.
 */
fun Route.userRoutes() {
    val authService by inject<AuthService>()

    route("/auth") {
        rateLimit(RateLimitName("auth")) {
            post("/register") {
                val request = call.receive<RegisterRequest>()
                val response = authService.register(request)
                call.respond(HttpStatusCode.Created, response)
            }

            post("/login") {
                val request = call.receive<LoginRequest>()
                val response = authService.login(request)
                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}
