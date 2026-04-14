package com.tablet.backend.exception

import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.io.buffer.DataBufferFactory
import java.time.LocalDateTime

data class ErrorResponse(
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
)

@Component
@Order(-2)
class GlobalExceptionHandler(
    private val objectMapper: ObjectMapper,
) : ErrorWebExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> {
        val response = exchange.response
        val path = exchange.request.path.value()

        val (httpStatus, message) = when (ex) {
            is ResourceNotFoundException -> HttpStatus.NOT_FOUND to ex.message
            is DuplicateResourceException -> HttpStatus.CONFLICT to ex.message
            is InvalidRequestException -> HttpStatus.BAD_REQUEST to ex.message
            is UnauthorizedException -> HttpStatus.UNAUTHORIZED to ex.message
            is ForbiddenException -> HttpStatus.FORBIDDEN to ex.message
            else -> {
                log.error("[GlobalExceptionHandler] 처리되지 않은 예외 발생: path=$path", ex)
                HttpStatus.INTERNAL_SERVER_ERROR to "서버 내부 오류가 발생했습니다."
            }
        }

        val errorResponse = ErrorResponse(
            status = httpStatus.value(),
            error = httpStatus.reasonPhrase,
            message = message ?: "알 수 없는 오류",
            path = path,
        )

        response.statusCode = httpStatus
        response.headers.contentType = MediaType.APPLICATION_JSON

        val bytes = objectMapper.writeValueAsBytes(errorResponse)
        val buffer = response.bufferFactory().wrap(bytes)
        return response.writeWith(Mono.just(buffer))
    }
}
