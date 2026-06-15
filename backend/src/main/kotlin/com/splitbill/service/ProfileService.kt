package com.splitbill.service

import com.splitbill.data.UserRepository
import com.splitbill.exceptions.InternalException
import com.splitbill.exceptions.NotFoundException
import com.splitbill.models.MessageResponse
import com.splitbill.models.ProfileResponse
import com.splitbill.models.UpdateBankInfoRequest

/**
 * ProfileService — chứa logic quản lý thông tin cá nhân & ngân hàng.
 */
class ProfileService(
    private val userRepository: UserRepository
) {

    suspend fun getMyProfile(userId: String): ProfileResponse {
        val user = userRepository.findUserById(userId)
            ?: throw NotFoundException("Không tìm thấy người dùng")

        return ProfileResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            avatarUrl = user.avatarUrl,
            bankCode = user.bankCode,
            accountNumber = user.accountNumber,
            accountName = user.accountName
        )
    }

    suspend fun getUserProfile(targetUserId: String): ProfileResponse {
        val user = userRepository.findUserById(targetUserId)
            ?: throw NotFoundException("Không tìm thấy người dùng")

        return ProfileResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            avatarUrl = user.avatarUrl,
            bankCode = user.bankCode,
            accountNumber = user.accountNumber,
            accountName = user.accountName
        )
    }

    suspend fun updateBankInfo(userId: String, request: UpdateBankInfoRequest): String {
        val success = userRepository.updateBankInfo(
            userId = userId,
            bankCode = request.bankCode.uppercase().trim(),
            accountNumber = request.accountNumber.trim(),
            accountName = request.accountName.trim().uppercase()
        )

        if (!success) {
            throw InternalException("Lỗi khi cập nhật thông tin ngân hàng")
        }

        return "Cập nhật thông tin ngân hàng thành công"
    }
}
