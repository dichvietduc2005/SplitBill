package com.splitbill

import com.splitbill.auth.JwtConfig
import com.splitbill.data.DatabaseFactory
import com.splitbill.di.appModule
import com.splitbill.plugins.configureRateLimiting
import com.splitbill.plugins.configureRequestValidation
import com.splitbill.plugins.configureStatusPages
import com.splitbill.routes.billRoutes
import com.splitbill.routes.groupRoutes
import com.splitbill.routes.profileRoutes
import com.splitbill.routes.userRoutes
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    // Khởi tạo Database và tự động tạo bảng
    DatabaseFactory.init()

    // Capture ApplicationEnvironment trước khi truyền vào Koin
    val appEnvironment = this.environment

    // ==========================================
    // PHASE 2: Cài đặt Koin Dependency Injection
    // ==========================================
    install(Koin) {
        slf4jLogger()
        modules(
            appModule,
            module {
                single { appEnvironment }
            }
        )
    }

    // ==========================================
    // PHASE 1: Cài đặt các Plugin bảo mật & xử lý lỗi
    // ==========================================

    // StatusPages — Global Exception Handler (JSON chuẩn)
    configureStatusPages()

    // RequestValidation — Tự động validate request body
    configureRequestValidation()

    // Rate Limiting — Chống brute-force
    configureRateLimiting()

    // JSON Serialization
    install(ContentNegotiation) {
        json()
    }

    // JWT Authentication — đọc config từ application.conf (không hardcode)
    val jwtConfig by inject<JwtConfig>()
    install(Authentication) {
        jwt("auth-jwt") {
            verifier(jwtConfig.verifier)
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

    // ==========================================
    // Đăng ký các API Routes
    // ==========================================
    val authService by inject<com.splitbill.service.AuthService>()
    val groupService by inject<com.splitbill.service.GroupService>()
    val billService by inject<com.splitbill.service.BillService>()
    val profileService by inject<com.splitbill.service.ProfileService>()

    routing {
        get("/") {
            call.respondText("Welcome to Split Bill API!")
        }

        // ==========================================
        // PHASE 3: Swagger API Documentation
        // ==========================================
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")

        route("/api") {
            userRoutes(authService)

            // Tất cả API bên dưới yêu cầu JWT token
            authenticate("auth-jwt") {
                get("/protected") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.getClaim("id").asString()
                    call.respondText("Bạn đã đăng nhập thành công! UserID của bạn là: $userId")
                }

                // API Quản lý Nhóm
                groupRoutes(groupService)

                // API Quản lý Hóa đơn & Tối giản nợ
                billRoutes(billService)

                // API Profile & Ngân hàng (VietQR)
                profileRoutes(profileService)
            }
        }
    }
}
