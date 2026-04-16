package com.tablet.backend.service

import com.tablet.backend.domain.user.User
import com.tablet.backend.dto.user.CreateUserRequest
import com.tablet.backend.dto.user.UpdateUserRequest
import com.tablet.backend.dto.user.UserResponse
import com.tablet.backend.exception.DuplicateResourceException
import com.tablet.backend.exception.ResourceNotFoundException
import com.tablet.backend.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    suspend fun getAllUsers(): Flow<UserResponse> =
        userRepository.findAllByIsActiveTrue()
            .map { UserResponse.from(it) }

    suspend fun getUserById(id: Long): UserResponse {
        val user = userRepository.findById(id)
            ?: throw ResourceNotFoundException("User", id)
        return UserResponse.from(user)
    }

    suspend fun getUserByEmail(email: String): UserResponse {
        val user = userRepository.findByEmail(email)
            ?: throw ResourceNotFoundException("User", "email=$email")
        return UserResponse.from(user)
    }

    @Transactional
    suspend fun createUser(request: CreateUserRequest): UserResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw DuplicateResourceException("이미 사용 중인 이메일입니다: ${request.email}")
        }
        if (userRepository.existsByUsername(request.username)) {
            throw DuplicateResourceException("이미 사용 중인 사용자명입니다: ${request.username}")
        }

        val newUser = User(
            username = request.username,
            email = request.email,
            password = request.password,
        )

        val savedUser = userRepository.save(newUser)
        return UserResponse.from(savedUser)
    }

    @Transactional
    suspend fun updateUser(id: Long, request: UpdateUserRequest): UserResponse {
        val existingUser = userRepository.findById(id)
            ?: throw ResourceNotFoundException("User", id)

        request.email?.let { newEmail ->
            if (newEmail != existingUser.email && userRepository.existsByEmail(newEmail)) {
                throw DuplicateResourceException("이미 사용 중인 이메일입니다: $newEmail")
            }
        }

        val updatedUser = existingUser.copy(
            username = request.username ?: existingUser.username,
            email = request.email ?: existingUser.email,
        )

        val savedUser = userRepository.save(updatedUser)
        return UserResponse.from(savedUser)
    }

    @Transactional
    suspend fun deleteUser(id: Long) {
        val existingUser = userRepository.findById(id)
            ?: throw ResourceNotFoundException("User", id)

        val deactivatedUser = existingUser.copy(isActive = false)
        userRepository.save(deactivatedUser)
    }
}
