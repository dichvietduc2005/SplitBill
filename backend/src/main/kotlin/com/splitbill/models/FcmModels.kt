package com.splitbill.models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterFcmTokenRequest(
    val token: String
)
