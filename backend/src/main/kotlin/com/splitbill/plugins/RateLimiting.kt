package com.splitbill.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import kotlin.time.Duration.Companion.minutes

/**
 * Cài đặt Rate Limiting — chống brute-force và spam API.
 *
 * Cấu hình:
 * - "auth": Giới hạn 10 request/phút cho endpoint đăng nhập và đăng ký.
 *   Rate key dựa trên IP của client.
 */
fun Application.configureRateLimiting() {
    install(RateLimit) {
        register(RateLimitName("auth")) {
            rateLimiter(limit = 10, refillPeriod = 1.minutes)
        }
    }
}
