package com.tablet.backend.dto.document

data class TableExtractionResponse(
    val totalRows: Int,
    val rows: List<TableRowResult>,
)

data class TableRowResult(
    // 행 번호 (0부터 시작, 헤더 행 포함)
    val rowIndex: Int,
    // 첫 번째 컬럼 값
    val firstColumn: String,
    // 마지막 컬럼 값
    val lastColumn: String,
)
