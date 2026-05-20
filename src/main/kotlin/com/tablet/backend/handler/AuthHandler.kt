package com.tablet.backend.handler

import com.tablet.backend.dto.auth.LoginRequest
import com.tablet.backend.dto.auth.RegisterRequest
import com.tablet.backend.service.AuthService
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyValueAndAwait

@Component
class AuthHandler(
    private val authService: AuthService,
) {
    suspend fun register(request: ServerRequest): ServerResponse {
        val body = request.awaitBody<RegisterRequest>()
        val response = authService.register(body)
        return ServerResponse.status(201).bodyValueAndAwait(response)
    }

    suspend fun login(request: ServerRequest): ServerResponse {
        val body = request.awaitBody<LoginRequest>()
        val response = authService.login(body)
        return ServerResponse.ok().bodyValueAndAwait(response)
    }
}
