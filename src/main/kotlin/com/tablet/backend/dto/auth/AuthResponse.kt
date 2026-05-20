package com.tablet.backend.dto.auth

import com.tablet.backend.domain.user.User

data class RegisterResponse(
    val userId: String,
    val passExpireDate: String,
    val createDttm: String,
) {
    companion object {
        fun from(user: User): RegisterResponse = RegisterResponse(
            userId = user.userId,
            passExpireDate = user.passExpireDate,
            createDttm = user.createDttm,
        )
    }
}

data class LoginResponse(
    val userId: String,
    val accessToken: String,
    val tokenType: String = "bearer",
    val expiresIn: Long,
)
