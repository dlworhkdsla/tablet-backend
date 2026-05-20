package com.tablet.backend.router

import com.tablet.backend.handler.AuthHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class AuthRouter(
    private val authHandler: AuthHandler,
) {
    @Bean
    fun authRoutes() = coRouter {
        "/api/v1/auth".nest {
            accept(MediaType.APPLICATION_JSON).nest {
                POST("/register", authHandler::register)
                POST("/login", authHandler::login)
            }
        }
    }
}
