package com.splitbill.service

import com.splitbill.auth.JwtConfig
import com.splitbill.data.UserRepository
import com.splitbill.exceptions.ConflictException
import com.splitbill.exceptions.UnauthorizedException
import com.splitbill.models.AuthResponse
import com.splitbill.models.LoginRequest
import com.splitbill.models.RegisterRequest
import org.mindrot.jbcrypt.BCrypt

/**
 * AuthService — chứa toàn bộ logic xác thực:
 * - Đăng ký (kiểm tra trùng, hash password, tạo user, generate token)
 * - Đăng nhập (kiểm tra credential, generate token)
 */
class AuthService(
    private val userRepository: UserRepository,
    private val jwtConfig: JwtConfig
) {

    suspend fun register(request: RegisterRequest): AuthResponse {
        // Kiểm tra username đã tồn tại
        if (userRepository.findUserByUsername(request.username) != null) {
            throw ConflictException("Username đã tồn tại")
        }

        // Kiểm tra email đã tồn tại
        if (userRepository.findUserByEmail(request.email) != null) {
            throw ConflictException("Email đã tồn tại")
        }

        // Hash password
        val hash = BCrypt.hashpw(request.password, BCrypt.gensalt())

        // Tạo user
        val newUser = userRepository.createUser(request.username, request.email, hash)
            ?: throw com.splitbill.exceptions.InternalException("Lỗi server khi tạo user")

        val token = jwtConfig.generateToken(newUser.id)
        return AuthResponse(token, newUser.username, "Đăng ký thành công!")
    }

    suspend fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findUserByUsername(request.username)
        if (user == null || !BCrypt.checkpw(request.password, user.passwordHash)) {
            throw UnauthorizedException("Sai username hoặc mật khẩu")
        }

        val token = jwtConfig.generateToken(user.id)
        return AuthResponse(token, user.username, "Đăng nhập thành công!")
    }
}
