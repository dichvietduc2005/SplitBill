package com.splitbill.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

object JwtConfig {
    private const val secret = "splitbill-super-secret-key-2026"
    private const val issuer = "splitbill-server"
    private const val validityInMs = 36_000_00 * 24 * 7 // 7 days

    val algorithm = Algorithm.HMAC512(secret)

    val verifier = JWT
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
