package com.tablet.backend.exception

import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDateTime

data class ErrorResponse(
    // LocalDateTime 대신 String으로 직렬화하여 JavaTimeModule 의존성 제거
    val timestamp: String = LocalDateTime.now().toString(),
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

        // 응답이 이미 커밋된 경우 추가 처리 불가
        if (response.isCommitted) {
            log.warn("[GlobalExceptionHandler] 응답이 이미 커밋됨, 에러 처리 불가: path=$path")
            return Mono.empty()
        }

        val (httpStatus, message) = when (ex) {
            is ResourceNotFoundException -> HttpStatus.NOT_FOUND to ex.message
            is DuplicateResourceException -> HttpStatus.CONFLICT to ex.message
            is InvalidRequestException -> HttpStatus.BAD_REQUEST to ex.message
            is UnauthorizedException -> HttpStatus.UNAUTHORIZED to ex.message
            is ForbiddenException -> HttpStatus.FORBIDDEN to ex.message
            is AzureServiceException -> {
                log.error("[GlobalExceptionHandler] Azure 서비스 오류: path=$path, message=${ex.message}", ex)
                HttpStatus.BAD_GATEWAY to "Azure 서비스 오류: ${ex.message}"
            }
            is DocumentAnalysisException -> {
                log.error("[GlobalExceptionHandler] 문서 분석 오류: path=$path, message=${ex.message}", ex)
                HttpStatus.UNPROCESSABLE_ENTITY to ex.message
            }
            else -> {
                log.error("[GlobalExceptionHandler] 처리되지 않은 예외 발생: path=$path", ex)
                HttpStatus.INTERNAL_SERVER_ERROR to "서버 내부 오류가 발생했습니다."
            }
        }

        val errorResponse = ErrorResponse(
            status = httpStatus.value(),
            error = httpStatus.reasonPhrase,
            message = message,
            path = path,
        )

        response.statusCode = httpStatus
        response.headers.contentType = MediaType.APPLICATION_JSON

        // JSON 직렬화 실패 시 하드코딩된 fallback 응답으로 HTML 반환 방지
        return try {
            val bytes = objectMapper.writeValueAsBytes(errorResponse)
            val buffer = response.bufferFactory().wrap(bytes)
            response.writeWith(Mono.just(buffer))
        } catch (e: Exception) {
            log.error("[GlobalExceptionHandler] JSON 직렬화 실패, fallback 응답 사용: ${e.message}")
            val fallback = """{"status":${httpStatus.value()},"error":"${httpStatus.reasonPhrase}","message":"$message","path":"$path","timestamp":"${LocalDateTime.now()}"}"""
            val buffer = response.bufferFactory().wrap(fallback.toByteArray(Charsets.UTF_8))
            response.writeWith(Mono.just(buffer))
        }
    }
}
