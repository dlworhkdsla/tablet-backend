package com.tablet.backend.service

import com.tablet.backend.config.AzureProperties
import com.tablet.backend.dto.document.AiAnalysisResult
import com.tablet.backend.dto.document.DocumentAnalysisResponse
import com.tablet.backend.dto.document.TokenUsage
import com.tablet.backend.exception.InvalidRequestException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DocumentAnalysisService(
    private val documentIntelligenceService: DocumentIntelligenceService,
    private val azureOpenAIService: AzureOpenAIService,
    private val azureProperties: AzureProperties,
) {
    private val log = LoggerFactory.getLogger(DocumentAnalysisService::class.java)

    suspend fun analyze(
        fileName: String,
        imageBytes: ByteArray,
        contentType: String,
    ): DocumentAnalysisResponse {
        val startTime = System.currentTimeMillis()
        log.info("문서 분석 시작 - 파일명: $fileName, 크기: ${imageBytes.size} bytes, 형식: $contentType")

        // 1단계: Azure Document Intelligence OCR
        val diResult = documentIntelligenceService.analyze(imageBytes, contentType)
        log.info("Azure DI OCR 완료 - 추출 텍스트 길이: ${diResult.content.length} chars")

        if (diResult.content.isBlank()) {
            throw InvalidRequestException("문서에서 텍스트를 추출할 수 없습니다. 이미지가 선명한지 확인해주세요.")
        }

        // 2단계: Azure OpenAI 분석 (content만 전달)
        val (nutritionInfo, tokenUsage) = azureOpenAIService.analyze(diResult.content)

        val processingTimeMs = System.currentTimeMillis() - startTime
        log.info("문서 분석 완료 - 소요 시간: ${processingTimeMs}ms")

        return DocumentAnalysisResponse(
            extractedText = diResult.content,
            aiAnalysis = AiAnalysisResult(
                nutrition = nutritionInfo,
                model = azureProperties.openai.deploymentName,
                tokenUsage = tokenUsage?.let {
                    TokenUsage(
                        promptTokens = it.promptTokens,
                        completionTokens = it.completionTokens,
                        totalTokens = it.totalTokens,
                    )
                },
            ),
            processingTimeMs = processingTimeMs,
        )
    }
}
