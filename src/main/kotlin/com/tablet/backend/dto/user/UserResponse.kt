package com.tablet.backend.dto.user

import com.tablet.backend.domain.user.User
import com.tablet.backend.domain.user.UserRole
import java.time.LocalDateTime

data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val role: UserRole,
    val isActive: Boolean,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(user: User): UserResponse = UserResponse(
            id = user.id!!,
            username = user.username,
            email = user.email,
            role = user.role,
            isActive = user.isActive,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
        )
    }
}
