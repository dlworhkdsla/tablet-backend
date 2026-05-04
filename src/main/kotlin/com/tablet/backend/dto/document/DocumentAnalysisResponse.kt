package com.tablet.backend.dto.document

import java.util.UUID

data class DocumentAnalysisResponse(
    val requestId: String = UUID.randomUUID().toString(),
    val status: String = "success",
    val extractedText: String,
    val aiAnalysis: AiAnalysisResult,
    val processingTimeMs: Long,
)

data class AiAnalysisResult(
    val nutrition: NutritionInfo?,
    val model: String,
    val tokenUsage: TokenUsage? = null,
)

data class NutritionInfo(
    val productName: String?,
    val servingUnit: String = "100g",
    val nutrients: List<NutrientItem> = emptyList(),
)

data class NutrientItem(
    val name: String,
    val value: Double?,
    val unit: String,
)

data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
)
