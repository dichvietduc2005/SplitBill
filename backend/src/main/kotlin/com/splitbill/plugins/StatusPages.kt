package com.splitbill.plugins

import com.splitbill.exceptions.AppException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable

/**
 * Cấu trúc JSON trả về khi có lỗi — chuẩn mực cho toàn bộ API.
 */
@Serializable
data class ErrorResponse(
    val status: Int,
    val message: String,
    val code: String
)

/**
 * Cài đặt StatusPages plugin — bắt tất cả exception và trả về JSON chuẩn.
 * Không bao giờ lộ stack trace hoặc thông tin nội bộ ra ngoài.
 */
fun Application.configureStatusPages() {
    val logger = this.log  // Capture Application logger cho dùng bên trong lambda

    install(StatusPages) {

        // 1. Bắt tất cả AppException (custom exception của chúng ta)
        exception<AppException> { call, cause ->
            call.respond(
                cause.statusCode,
                ErrorResponse(
                    status = cause.statusCode.value,
                    message = cause.message,
                    code = cause.errorCode
                )
            )
        }

        // 2. Bắt lỗi validation từ RequestValidation plugin
        exception<RequestValidationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    status = 400,
                    message = cause.reasons.joinToString("; "),
                    code = "VALIDATION_ERROR"
                )
            )
        }

        // 3. Bắt lỗi thiếu tham số bắt buộc (MissingRequestParameterException)
        exception<MissingRequestParameterException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    status = 400,
                    message = "Thiếu tham số bắt buộc: ${cause.parameterName}",
                    code = "MISSING_PARAMETER"
                )
            )
        }

        // 4. Bắt lỗi parse JSON body bị sai format
        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    status = 400,
                    message = "Dữ liệu gửi lên không đúng định dạng: ${cause.message ?: "Unknown"}",
                    code = "BAD_REQUEST"
                )
            )
        }

        // 5. Bắt tất cả exception không mong muốn
        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    status = 500,
                    message = "Lỗi 500: ${cause.message} (${cause.javaClass.simpleName})",
                    code = "INTERNAL_ERROR"
                )
            )
        }
    }
}
