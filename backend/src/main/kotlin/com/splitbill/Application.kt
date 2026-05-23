package com.splitbill

import com.splitbill.auth.JwtConfig
import com.splitbill.data.DatabaseFactory
import com.splitbill.routes.userRoutes
import com.splitbill.routes.groupRoutes
import com.splitbill.routes.billRoutes
import com.splitbill.routes.profileRoutes
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    // Khởi tạo Database và tự động tạo bảng
    DatabaseFactory.init()

    // Cấu hình JSON Serialization
    install(ContentNegotiation) {
        json()
    }

    // Cấu hình JWT Authentication
    install(Authentication) {
        jwt("auth-jwt") {
            verifier(JwtConfig.verifier)
            validate { credential ->
                if (credential.payload.getClaim("id").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { defaultScheme, realm ->
                call.respond(io.ktor.http.HttpStatusCode.Unauthorized, "Token không hợp lệ hoặc đã hết hạn")
            }
        }
    }

    // Đăng ký các API Routes
    routing {
        get("/") {
            call.respondText("Welcome to Split Bill API!")
        }
        
        route("/api") {
            userRoutes()

            // Tất cả API bên dưới yêu cầu JWT token
            authenticate("auth-jwt") {
                get("/protected") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.getClaim("id").asString()
                    call.respondText("Bạn đã đăng nhập thành công! UserID của bạn là: $userId")
                }

                // API Quản lý Nhóm
                groupRoutes()

                // API Quản lý Hóa đơn & Tối giản nợ
                billRoutes()

                // API Profile & Ngân hàng (VietQR)
                profileRoutes()
            }
        }
    }
}
