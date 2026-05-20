package com.tablet.backend.handler

import com.tablet.backend.service.UserService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait

@Component
@Profile("local")
class UserHandler(
    private val userService: UserService,
) {
    suspend fun getUserById(request: ServerRequest): ServerResponse {
        val userId = request.pathVariable("userId")
        val user = userService.getUserById(userId)
        return ServerResponse.ok().bodyValueAndAwait(user)
    }
}
