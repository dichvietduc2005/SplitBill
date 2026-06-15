package com.splitbill.exceptions

import io.ktor.http.*

/**
 * Base class cho tất cả exception nghiệp vụ của ứng dụng.
 * Mỗi exception tự mang theo HTTP status code và error code riêng,
 * giúp StatusPages plugin trả về JSON chuẩn mực.
 */
open class AppException(
    val statusCode: HttpStatusCode,
    override val message: String,
    val errorCode: String
) : RuntimeException(message)

// ==========================================
// 400 - Bad Request
// ==========================================
class ValidationException(
    message: String,
    errorCode: String = "VALIDATION_ERROR"
) : AppException(HttpStatusCode.BadRequest, message, errorCode)

// ==========================================
// 401 - Unauthorized
// ==========================================
class UnauthorizedException(
    message: String = "Sai thông tin đăng nhập",
    errorCode: String = "UNAUTHORIZED"
) : AppException(HttpStatusCode.Unauthorized, message, errorCode)

// ==========================================
// 403 - Forbidden
// ==========================================
class ForbiddenException(
    message: String = "Bạn không có quyền truy cập",
    errorCode: String = "FORBIDDEN"
) : AppException(HttpStatusCode.Forbidden, message, errorCode)

// ==========================================
// 404 - Not Found
// ==========================================
class NotFoundException(
    message: String = "Không tìm thấy tài nguyên",
    errorCode: String = "NOT_FOUND"
) : AppException(HttpStatusCode.NotFound, message, errorCode)

// ==========================================
// 409 - Conflict
// ==========================================
class ConflictException(
    message: String,
    errorCode: String = "CONFLICT"
) : AppException(HttpStatusCode.Conflict, message, errorCode)

// ==========================================
// 429 - Too Many Requests
// ==========================================
class RateLimitException(
    message: String = "Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau.",
    errorCode: String = "RATE_LIMITED"
) : AppException(HttpStatusCode.TooManyRequests, message, errorCode)

// ==========================================
// 500 - Internal Server Error
// ==========================================
class InternalException(
    message: String = "Lỗi máy chủ nội bộ",
    errorCode: String = "INTERNAL_ERROR"
) : AppException(HttpStatusCode.InternalServerError, message, errorCode)
