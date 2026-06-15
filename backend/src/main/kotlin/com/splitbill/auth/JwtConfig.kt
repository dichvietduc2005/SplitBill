package com.splitbill.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import java.util.*

/**
 * JWT Configuration — đọc secret và issuer từ application.conf.
 *
 * Không còn hardcode secret key trong source code.
 * Ở Production, override bằng biến môi trường JWT_SECRET.
 */
class JwtConfig(environment: ApplicationEnvironment) {
    private val secret: String = environment.config.property("jwt.secret").getString()
    private val issuer: String = environment.config.property("jwt.issuer").getString()
    private val validityDays: Int = environment.config.property("jwt.validityDays").getString().toInt()
    private val validityInMs: Long = validityDays.toLong() * 24 * 3_600_000

    val algorithm: Algorithm = Algorithm.HMAC512(secret)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .build()

    fun generateToken(userId: String): String {
        return JWT.create()
            .withSubject("Authentication")
            .withIssuer(issuer)
            .withClaim("id", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
            .sign(algorithm)
    }
}
