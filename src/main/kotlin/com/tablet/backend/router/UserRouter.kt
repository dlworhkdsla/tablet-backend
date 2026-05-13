package com.tablet.backend.router

import com.tablet.backend.handler.UserHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.coRouter

@Configuration
@Profile("local")
class UserRouter(
    private val userHandler: UserHandler,
) {
    @Bean
    fun userRoutes() = coRouter {
        "/api/v1/users".nest {
            accept(MediaType.APPLICATION_JSON).nest {
                GET("", userHandler::getAllUsers)
                GET("/{id}", userHandler::getUserById)
                POST("", userHandler::createUser)
                PUT("/{id}", userHandler::updateUser)
                DELETE("/{id}", userHandler::deleteUser)
            }
        }
    }
}