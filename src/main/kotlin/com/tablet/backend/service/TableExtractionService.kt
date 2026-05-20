package com.tablet.backend.service

import com.tablet.backend.dto.document.TableExtractionResponse
import com.tablet.backend.dto.document.TableRowResult
import com.tablet.backend.exception.DocumentAnalysisException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TableExtractionService(
    private val documentIntelligenceService: DocumentIntelligenceService,
) {
    private val log = LoggerFactory.getLogger(TableExtractionService::class.java)

    suspend fun extractFirstAndLastColumns(
        imageBytes: ByteArray,
        contentType: String,
        // 헤더 행 스킵 여부 (true = 첫 번째 행 제외)
        skipHeader: Boolean = false,
    ): TableExtractionResponse {
        val diResult = documentIntelligenceService.analyze(imageBytes, contentType)

        if (diResult.tables.isEmpty()) {
            log.warn("Azure DI 결과에 테이블이 감지되지 않음")
            throw DocumentAnalysisException("이미지에서 표 구조를 감지하지 못했습니다. 이미지 품질을 확인해 주세요.")
        }

        // 셀 수가 가장 많은 테이블을 메인 테이블로 선택
        val mainTable = diResult.tables.maxByOrNull { it.cells.size }!!
        log.info("테이블 감지 - 행: ${mainTable.rowCount}, 열: ${mainTable.columnCount}, 셀: ${mainTable.cells.size}")

        // rowIndex → (columnIndex → content) 맵으로 변환
        val rowMap: Map<Int, Map<Int, String>> = mainTable.cells
            .groupBy { it.rowIndex }
            .mapValues { (_, cells) ->
                cells.associate { it.columnIndex to it.content }
            }

        val lastColIndex = mainTable.columnCount - 1
        val startRow = if (skipHeader) 1 else 0

        val rows = rowMap.entries
            .filter { (rowIndex, _) -> rowIndex >= startRow }
            .sortedBy { (rowIndex, _) -> rowIndex }
            .map { (rowIndex, colMap) ->
                TableRowResult(
                    rowIndex = rowIndex,
                    firstColumn = colMap[0]?.trim() ?: "",
                    lastColumn = colMap[lastColIndex]?.trim() ?: "",
                )
            }
            // 첫 번째 컬럼이 비어있는 행은 제외
            .filter { it.firstColumn.isNotBlank() }

        log.info("추출 완료 - 총 ${rows.size}개 행 (헤더 스킵: $skipHeader)")

        return TableExtractionResponse(
            totalRows = rows.size,
            rows = rows,
        )
    }
}
