package com.tablet.backend.repository

import com.tablet.backend.domain.user.User
import kotlinx.coroutines.flow.Flow
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
@Profile("db")
interface UserRepository : CoroutineCrudRepository<User, Long> {

    suspend fun findByEmail(email: String): User?

    suspend fun findByUsername(username: String): User?

    suspend fun existsByEmail(email: String): Boolean

    suspend fun existsByUsername(username: String): Boolean

    fun findAllByIsActiveTrue(): Flow<User>
}
