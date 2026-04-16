package com.tablet.backend.handler

import com.tablet.backend.dto.user.CreateUserRequest
import com.tablet.backend.dto.user.UpdateUserRequest
import com.tablet.backend.service.UserService
import kotlinx.coroutines.flow.Flow
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*

@Component
class UserHandler(
    private val userService: UserService,
) {
    suspend fun getAllUsers(request: ServerRequest): ServerResponse =
        ServerResponse.ok()
            .bodyAndAwait(userService.getAllUsers())

    suspend fun getUserById(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id").toLong()
        val user = userService.getUserById(id)
        return ServerResponse.ok().bodyValueAndAwait(user)
    }

    suspend fun createUser(request: ServerRequest): ServerResponse {
        val body = request.awaitBody<CreateUserRequest>()
        val createdUser = userService.createUser(body)
        return ServerResponse.status(201).bodyValueAndAwait(createdUser)
    }

    suspend fun updateUser(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id").toLong()
        val body = request.awaitBody<UpdateUserRequest>()
        val updatedUser = userService.updateUser(id, body)
        return ServerResponse.ok().bodyValueAndAwait(updatedUser)
    }

    suspend fun deleteUser(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id").toLong()
        userService.deleteUser(id)
        return ServerResponse.noContent().buildAndAwait()
    }
}
