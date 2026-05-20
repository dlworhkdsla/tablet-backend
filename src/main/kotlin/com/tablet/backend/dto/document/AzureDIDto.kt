package com.tablet.backend.dto.document

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DiOperationResult(
    val status: String,
    val analyzeResult: DiAnalyzeResult? = null,
    val error: DiError? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DiAnalyzeResult(
    val content: String = "",
    val tables: List<DiTable> = emptyList(),
)

// Azure DI 테이블 구조
@JsonIgnoreProperties(ignoreUnknown = true)
data class DiTable(
    val rowCount: Int = 0,
    val columnCount: Int = 0,
    val cells: List<DiCell> = emptyList(),
)

// Azure DI 셀 구조 — rowIndex, columnIndex 기반으로 행/열 특정
@JsonIgnoreProperties(ignoreUnknown = true)
data class DiCell(
    val rowIndex: Int = 0,
    val columnIndex: Int = 0,
    val content: String = "",
    val kind: String = "content",  // "columnHeader" | "rowHeader" | "content"
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DiError(
    val code: String = "",
    val message: String = "",
)
