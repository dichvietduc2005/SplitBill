package com.splitbill.plugins

import com.splitbill.models.*
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*

/**
 * Cài đặt RequestValidation plugin — tự động validate request body
 * trước khi nó tới handler. Nếu validation thất bại, StatusPages sẽ
 * bắt RequestValidationException và trả về JSON lỗi chuẩn mực.
 */
fun Application.configureRequestValidation() {
    install(RequestValidation) {

        // ==========================================
        // AUTH
        // ==========================================
        validate<RegisterRequest> { request ->
            val errors = mutableListOf<String>()
            if (request.username.isBlank()) errors.add("Username không được để trống")
            if (request.username.length < 3) errors.add("Username phải có ít nhất 3 ký tự")
            if (request.username.length > 50) errors.add("Username tối đa 50 ký tự")
            if (request.email.isBlank()) errors.add("Email không được để trống")
            if (!request.email.contains("@")) errors.add("Email không hợp lệ")
            if (request.password.isBlank()) errors.add("Mật khẩu không được để trống")
            if (request.password.length < 6) errors.add("Mật khẩu phải có ít nhất 6 ký tự")

            if (errors.isEmpty()) ValidationResult.Valid
            else ValidationResult.Invalid(errors)
        }

        validate<LoginRequest> { request ->
            val errors = mutableListOf<String>()
            if (request.username.isBlank()) errors.add("Username không được để trống")
            if (request.password.isBlank()) errors.add("Mật khẩu không được để trống")

            if (errors.isEmpty()) ValidationResult.Valid
            else ValidationResult.Invalid(errors)
        }

        // ==========================================
        // GROUP
        // ==========================================
        validate<CreateGroupRequest> { request ->
            if (request.name.isBlank()) ValidationResult.Invalid("Tên nhóm không được để trống")
            else if (request.name.length > 100) ValidationResult.Invalid("Tên nhóm tối đa 100 ký tự")
            else ValidationResult.Valid
        }

        validate<AddMemberRequest> { request ->
            if (request.usernameOrEmail.isBlank()) ValidationResult.Invalid("Username hoặc email không được để trống")
            else ValidationResult.Valid
        }

        // ==========================================
        // BILL
        // ==========================================
        validate<CreateBillRequest> { request ->
            val errors = mutableListOf<String>()
            if (request.description.isBlank()) errors.add("Mô tả hóa đơn không được để trống")
            if (request.description.length > 255) errors.add("Mô tả tối đa 255 ký tự")
            if (request.totalAmount <= 0) errors.add("Tổng tiền phải lớn hơn 0")
            if (request.splits.isEmpty()) errors.add("Phải có ít nhất 1 người chia nợ")
            if (request.groupId.isBlank()) errors.add("Thiếu groupId")
            if (request.paidByUserId.isBlank()) errors.add("Thiếu người trả tiền")

            // Kiểm tra tổng splits có khớp totalAmount
            if (request.splits.isNotEmpty() && request.totalAmount > 0) {
                val splitsTotal = request.splits.sumOf { it.amount }
                if (kotlin.math.abs(splitsTotal - request.totalAmount) > 0.01) {
                    errors.add("Tổng các khoản chia ($splitsTotal) không khớp tổng hóa đơn (${request.totalAmount})")
                }
            }

            // Kiểm tra từng split
            for (split in request.splits) {
                if (split.userId.isBlank()) errors.add("userId trong splits không được để trống")
                if (split.amount <= 0) errors.add("Số tiền chia cho mỗi người phải lớn hơn 0")
            }

            if (errors.isEmpty()) ValidationResult.Valid
            else ValidationResult.Invalid(errors)
        }

        // ==========================================
        // PROFILE
        // ==========================================
        validate<UpdateBankInfoRequest> { request ->
            val errors = mutableListOf<String>()
            if (request.bankCode.isBlank()) errors.add("Mã ngân hàng không được để trống")
            if (request.accountNumber.isBlank()) errors.add("Số tài khoản không được để trống")
            if (request.accountName.isBlank()) errors.add("Tên chủ tài khoản không được để trống")

            if (errors.isEmpty()) ValidationResult.Valid
            else ValidationResult.Invalid(errors)
        }
    }
}
