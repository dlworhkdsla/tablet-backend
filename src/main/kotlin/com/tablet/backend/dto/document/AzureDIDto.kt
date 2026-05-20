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
    // 페이지별 단어 목록 — 각 단어의 polygon 좌표로 기울기 계산에 사용
    val pages: List<DiPage> = emptyList(),
)

// 페이지 단위 데이터
@JsonIgnoreProperties(ignoreUnknown = true)
data class DiPage(
    val pageNumber: Int = 1,
    val words: List<DiWord> = emptyList(),
)

// 단어 단위 데이터 — polygon으로 기울기 계산, span으로 셀과 매핑
@JsonIgnoreProperties(ignoreUnknown = true)
data class DiWord(
    val content: String = "",
    // 바운딩 폴리곤: [x0,y0, x1,y1, x2,y2, x3,y3] 시계방향 4꼭짓점
    val polygon: List<Double> = emptyList(),
    val confidence: Double = 1.0,
    val span: DiSpan = DiSpan(),
)

// content 문자열 내 위치 (단어 ↔ 셀 매핑에 사용)
@JsonIgnoreProperties(ignoreUnknown = true)
data class DiSpan(
    val offset: Int = 0,
    val length: Int = 0,
)

// Azure DI 테이블 구조
@JsonIgnoreProperties(ignoreUnknown = true)
data class DiTable(
    val rowCount: Int = 0,
    val columnCount: Int = 0,
    val cells: List<DiCell> = emptyList(),
)

// Azure DI 셀 — spans로 해당 셀에 속한 단어들을 특정
@JsonIgnoreProperties(ignoreUnknown = true)
data class DiCell(
    val rowIndex: Int = 0,
    val columnIndex: Int = 0,
    val content: String = "",
    val kind: String = "content",
    val spans: List<DiSpan> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DiError(
    val code: String = "",
    val message: String = "",
)
