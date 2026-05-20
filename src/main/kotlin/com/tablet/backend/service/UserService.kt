package com.tablet.backend.service

import com.tablet.backend.dto.user.UserResponse
import com.tablet.backend.exception.ResourceNotFoundException
import com.tablet.backend.repository.UserRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Service
@Profile("local")
class UserService(
    private val userRepository: UserRepository,
) {
    suspend fun getUserById(userId: String): UserResponse {
        val user = userRepository.findByUserId(userId)
            ?: throw ResourceNotFoundException("User", userId)
        return UserResponse.from(user)
    }
}
