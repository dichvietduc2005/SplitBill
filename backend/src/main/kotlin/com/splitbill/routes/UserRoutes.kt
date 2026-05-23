package com.splitbill.routes

import com.splitbill.auth.JwtConfig
import com.splitbill.data.UserRepository
import com.splitbill.models.AuthResponse
import com.splitbill.models.LoginRequest
import com.splitbill.models.RegisterRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.mindrot.jbcrypt.BCrypt

fun Route.userRoutes() {
    route("/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            
            // Validate
            if (request.username.isBlank() || request.password.isBlank() || request.email.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Thiếu thông tin đăng ký")
                return@post
            }
            
            // Check existing user
            if (UserRepository.findUserByUsername(request.username) != null) {
                call.respond(HttpStatusCode.Conflict, "Username đã tồn tại")
                return@post
            }
            if (UserRepository.findUserByEmail(request.email) != null) {
                call.respond(HttpStatusCode.Conflict, "Email đã tồn tại")
                return@post
            }

            // Hash password
            val hash = BCrypt.hashpw(request.password, BCrypt.gensalt())
            
            // Save user
            val newUser = UserRepository.createUser(request.username, request.email, hash)
            if (newUser != null) {
                val token = JwtConfig.generateToken(newUser.id)
                call.respond(HttpStatusCode.Created, AuthResponse(token, newUser.username, "Đăng ký thành công!"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Lỗi server khi tạo user")
            }
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            
            val user = UserRepository.findUserByUsername(request.username)
            if (user == null || !BCrypt.checkpw(request.password, user.passwordHash)) {
                call.respond(HttpStatusCode.Unauthorized, "Sai username hoặc mật khẩu")
                return@post
            }
            
            val token = JwtConfig.generateToken(user.id)
            call.respond(HttpStatusCode.OK, AuthResponse(token, user.username, "Đăng nhập thành công!"))
        }
    }
}
