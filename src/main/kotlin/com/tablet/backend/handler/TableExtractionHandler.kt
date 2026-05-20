package com.tablet.backend.handler

import com.tablet.backend.exception.InvalidRequestException
import com.tablet.backend.service.TableExtractionService
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitMultipartData
import org.springframework.web.reactive.function.server.bodyValueAndAwait

@Component
class TableExtractionHandler(
    private val tableExtractionService: TableExtractionService,
) {
    private val log = LoggerFactory.getLogger(TableExtractionHandler::class.java)

    suspend fun extractTable(request: ServerRequest): ServerResponse {
        val multipartData = request.awaitMultipartData()

        val filePart = multipartData.getFirst("image") as? FilePart
            ?: throw InvalidRequestException(
                "'image' 키를 포함한 multipart/form-data 요청이 필요합니다.",
            )

        // skipHeader 쿼리 파라미터 (기본값: true — 보통 표 첫 행은 헤더)
        val skipHeader = request.queryParam("skipHeader")
            .map { it.equals("true", ignoreCase = true) }
            .orElse(true)

        log.debug("테이블 추출 요청 - 파일: ${filePart.filename()}, skipHeader: $skipHeader")

        val imageBytes = DataBufferUtils.join(filePart.content())
            .map { dataBuffer ->
                val bytes = ByteArray(dataBuffer.readableByteCount())
                dataBuffer.read(bytes)
                DataBufferUtils.release(dataBuffer)
                bytes
            }
            .awaitSingle()

        if (imageBytes.isEmpty()) {
            throw InvalidRequestException("업로드된 이미지 파일이 비어있습니다.")
        }

        val contentType = filePart.headers().contentType?.toString() ?: "application/octet-stream"

        val response = tableExtractionService.extractFirstAndLastColumns(
            imageBytes = imageBytes,
            contentType = contentType,
            skipHeader = skipHeader,
        )

        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValueAndAwait(response)
    }
}
