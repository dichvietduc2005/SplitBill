package com.splitbill.models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String 
)

@Serializable
data class AuthResponse(
    val token: String,
    val username: String,
    val message: String
)
