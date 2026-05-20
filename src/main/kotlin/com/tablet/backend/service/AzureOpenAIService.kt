package com.tablet.backend.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tablet.backend.config.AzureProperties
import com.tablet.backend.dto.document.NutritionInfo
import com.tablet.backend.dto.document.OpenAIChatRequest
import com.tablet.backend.dto.document.OpenAIChatResponse
import com.tablet.backend.dto.document.OpenAIMessage
import com.tablet.backend.dto.document.OpenAIUsage
import com.tablet.backend.exception.AzureServiceException
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody

@Service
class AzureOpenAIService(
    private val webClient: WebClient,
    private val azureProperties: AzureProperties,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(AzureOpenAIService::class.java)

    companion object {
        private const val SYSTEM_PROMPT = """당신은 식품 영양성분 추출 전문가입니다.
제공된 문서에서 영양성분 정보만 추출하여 반드시 아래 JSON 형식으로만 응답하세요.
다른 설명, 마크다운 코드블록, 추가 텍스트 없이 순수 JSON만 출력하세요.

{
  "productName": "제품명 (없으면 null)",
  "analysisAgent": "분석기관 (없으면 null)",
  "analysisDate": "분석일자 (없으면 null)",
  "servingUnit": "기준단위 (예: 100g, 1회제공량 등)",
  "nutrients": [
    {"name": "열량", "value": 143.61, "unit": "kcal"},
    {"name": "나트륨", "value": 176.25, "unit": "mg"},
    {"name": "탄수화물", "value": 19.31, "unit": "g"},
    {"name": "당류", "value": 1.81, "unit": "g"},
    {"name": "지방", "value": 3.97, "unit": "g"},
    {"name": "트랜스지방", "value": 0.00, "unit": "g"},
    {"name": "포화지방", "value": 1.43, "unit": "g"},
    {"name": "콜레스테롤", "value": 6.93, "unit": "mg"},
    {"name": "단백질", "value": 7.66, "unit": "g"}
  ]
}

영양성분 정보가 없으면 nutrients를 빈 배열로 반환하세요."""
    }

    suspend fun analyze(extractedText: String): Pair<NutritionInfo?, OpenAIUsage?> {
        val openAIProps = azureProperties.openai
        val completionUrl = buildCompletionUrl(openAIProps)

        log.info("Azure OpenAI 분석 시작 - 배포: ${openAIProps.deploymentName}, 입력 텍스트 길이: ${extractedText.length}")

        val request = OpenAIChatRequest(
            messages = listOf(
                OpenAIMessage(role = "system", content = SYSTEM_PROMPT),
                OpenAIMessage(
                    role = "user",
                    content = "다음 문서에서 추출된 텍스트를 분석해주세요:\n\n$extractedText",
                ),
            ),
            maxTokens = openAIProps.maxTokens,
            temperature = openAIProps.temperature,
        )

        try {
            val response = webClient.post()
                .uri(completionUrl)
                .header("api-key", openAIProps.key)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .awaitBody<OpenAIChatResponse>()

            val rawContent = response.choices.firstOrNull()?.message?.content
                ?: throw AzureServiceException("Azure OpenAI 응답에 콘텐츠가 없습니다.")

            log.info(
                "Azure OpenAI 분석 완료 - 완료 이유: ${response.choices.firstOrNull()?.finishReason}, " +
                    "토큰 사용량: ${response.usage?.totalTokens}",
            )

            val nutritionInfo = try {
                objectMapper.readValue<NutritionInfo>(rawContent)
            } catch (e: Exception) {
                log.warn("영양성분 JSON 파싱 실패, raw: $rawContent", e)
                null
            }

            return Pair(nutritionInfo, response.usage)
        } catch (e: AzureServiceException) {
            throw e
        } catch (e: WebClientResponseException) {
            log.error("Azure OpenAI 요청 실패 - status: ${e.statusCode}, body: ${e.responseBodyAsString}")
            throw AzureServiceException("Azure OpenAI 요청 실패 (${e.statusCode}): ${e.responseBodyAsString}")
        }
    }

    private fun buildCompletionUrl(openAIProps: AzureProperties.OpenAIProperties): String =
        "${openAIProps.endpoint.trimEnd('/')}/openai/deployments/${openAIProps.deploymentName}" +
            "/chat/completions?api-version=${openAIProps.apiVersion}"
}
