package com.splitbill.models

import kotlinx.serialization.Serializable

// ==========================================
// PROFILE REQUEST DTOs
// ==========================================

@Serializable
data class UpdateBankInfoRequest(
    val bankCode: String,       // Mã ngân hàng VietQR (VCB, TCB, MB, ...)
    val accountNumber: String,  // Số tài khoản
    val accountName: String     // Tên chủ tài khoản (hiển thị trên QR)
)

// ==========================================
// PROFILE RESPONSE DTOs
// ==========================================

@Serializable
data class ProfileResponse(
    val id: String,
    val username: String,
    val email: String,
    val avatarUrl: String?,
    // Thông tin ngân hàng cho VietQR
    val bankCode: String?,
    val accountNumber: String?,
    val accountName: String?
)
