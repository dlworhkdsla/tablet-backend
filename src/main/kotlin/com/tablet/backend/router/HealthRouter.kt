package com.tablet.backend.router

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.coRouter

// Render 헬스체크 및 루트 경로 요청 처리
@Configuration
class HealthRouter {

    @Bean
    fun healthRoutes() = coRouter {
        GET("/") {
            ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(mapOf("status" to "ok"))
        }
        HEAD("/") {
            ServerResponse.ok().buildAndAwait()
        }
        GET("/health") {
            ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(mapOf("status" to "ok"))
        }
    }
}
