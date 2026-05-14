package com.tablet.backend.service

import com.tablet.backend.config.AzureProperties
import com.tablet.backend.dto.document.DiAnalyzeResult
import com.tablet.backend.dto.document.DiOperationResult
import com.tablet.backend.exception.AzureServiceException
import com.tablet.backend.exception.InvalidRequestException
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import java.net.URI
import java.util.Base64

@Service
class DocumentIntelligenceService(
    private val webClient: WebClient,
    private val azureProperties: AzureProperties,
) {
    private val log = LoggerFactory.getLogger(DocumentIntelligenceService::class.java)

    companion object {
        private val SUPPORTED_CONTENT_TYPES = setOf(
            "image/jpeg", "image/jpg", "image/png", "image/bmp",
            "image/tiff", "image/heif", "application/pdf",
        )
    }

    suspend fun analyze(imageBytes: ByteArray, contentType: String): DiAnalyzeResult {
        validateContentType(contentType)

        val diProps = azureProperties.documentIntelligence
        log.info("Azure DI 분석 시작 - 모델: ${diProps.modelId}, 파일 크기: ${imageBytes.size} bytes")

        val base64Image = Base64.getEncoder().encodeToString(imageBytes)
        val analyzeUrl = buildAnalyzeUrl(diProps)

        val operationLocation = submitDocument(analyzeUrl, base64Image, diProps.key)
        log.debug("Azure DI 폴링 시작 - operationLocation: $operationLocation")

        return pollForResult(operationLocation, diProps)
    }

    private suspend fun submitDocument(
        analyzeUrl: String,
        base64Image: String,
        apiKey: String,
    ): String {
        log.debug("Azure DI 문서 제출 - URL: $analyzeUrl")
        try {
            val response = webClient.post()
                // URI.create()를 사용하여 Spring URI 템플릿 파싱 우회
                .uri(URI.create(analyzeUrl))
                .header("Ocp-Apim-Subscription-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(mapOf("base64Source" to base64Image))
                .retrieve()
                .toBodilessEntity()
                .awaitSingle()

            return response.headers.getFirst("Operation-Location")
                ?: throw AzureServiceException("Azure DI 응답에 Operation-Location 헤더가 없습니다.")
        } catch (e: WebClientResponseException) {
            log.error("Azure DI 문서 제출 실패 - status: ${e.statusCode}, body: ${e.responseBodyAsString}")
            throw AzureServiceException("Azure Document Intelligence 요청 실패 (${e.statusCode}): ${e.responseBodyAsString}")
        }
    }

    private suspend fun pollForResult(
        operationLocation: String,
        diProps: AzureProperties.DocumentIntelligenceProperties,
    ): DiAnalyzeResult {
        repeat(diProps.maxPollAttempts) { attempt ->
            delay(diProps.pollIntervalMs)

            try {
                val result = webClient.get()
                    // URI.create()를 사용하여 Spring URI 템플릿 파싱 우회
                    .uri(URI.create(operationLocation))
                    .header("Ocp-Apim-Subscription-Key", diProps.key)
                    .retrieve()
                    .awaitBody<DiOperationResult>()

                log.debug("Azure DI 폴링 - 상태: ${result.status} (${attempt + 1}/${diProps.maxPollAttempts})")

                when (result.status) {
                    "succeeded" -> {
                        log.info("Azure DI 분석 완료 (총 ${attempt + 1}회 폴링)")
                        return result.analyzeResult
                            ?: throw AzureServiceException("Azure DI 분석 결과가 비어있습니다.")
                    }
                    "failed" -> {
                        val errorMsg = result.error?.message ?: "알 수 없는 오류"
                        val errorCode = result.error?.code ?: ""
                        log.error("Azure DI 분석 실패 - code: $errorCode, message: $errorMsg")
                        throw AzureServiceException("Azure DI 분석 실패 [$errorCode]: $errorMsg")
                    }
                    else -> log.debug("Azure DI 분석 진행 중 (상태: ${result.status})")
                }
            } catch (e: AzureServiceException) {
                throw e
            } catch (e: WebClientResponseException) {
                log.error("Azure DI 폴링 요청 실패 - status: ${e.statusCode}, body: ${e.responseBodyAsString}")
                throw AzureServiceException("Azure DI 폴링 실패 (${e.statusCode}): ${e.message}")
            }
        }

        val timeoutSec = diProps.maxPollAttempts * diProps.pollIntervalMs / 1000
        throw AzureServiceException("Azure DI 분석 시간 초과 (${timeoutSec}초 초과)")
    }

    private fun buildAnalyzeUrl(diProps: AzureProperties.DocumentIntelligenceProperties): String =
        "${diProps.endpoint.trimEnd('/')}/documentintelligence/documentModels/${diProps.modelId}:analyze" +
            "?api-version=${diProps.apiVersion}"

    private fun validateContentType(contentType: String) {
        val normalizedType = contentType.lowercase().split(";").first().trim()
        if (normalizedType !in SUPPORTED_CONTENT_TYPES) {
            throw InvalidRequestException(
                "지원하지 않는 파일 형식입니다: '$contentType'. " +
                    "지원 형식: ${SUPPORTED_CONTENT_TYPES.joinToString(", ")}",
            )
        }
    }
}
