package com.tablet.backend.dto.document

data class TableExtractionResponse(
    val totalRows: Int,
    val rows: List<TableRowResult>,
)

data class TableRowResult(
    val rowIndex: Int,
    // 첫 번째 컬럼 (품목코드)
    val firstColumn: String,
    // 마지막 컬럼 — 워터마크 제거 후 정제된 값
    val lastColumn: String,
    // 마지막 컬럼 원본값 (워터마크 포함 가능, 검증용)
    val rawLastColumn: String,
)
