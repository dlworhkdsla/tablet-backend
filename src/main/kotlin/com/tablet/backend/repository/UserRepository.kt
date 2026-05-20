package com.tablet.backend.repository

import com.tablet.backend.domain.user.User
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : CoroutineCrudRepository<User, String> {

    suspend fun existsByUserId(userId: String): Boolean

    suspend fun findByUserId(userId: String): User?
}
