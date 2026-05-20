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

    // 순수 숫자
    private val pureNumber = Regex("^\\d+$")
    // 단일 알파벳 대문자 (X, P 등 유효 기호)
    private val singleLetter = Regex("^[A-Z]$")

    suspend fun extractFirstAndLastColumns(
        imageBytes: ByteArray,
        contentType: String,
        skipHeader: Boolean = false,
    ): TableExtractionResponse {
        val diResult = documentIntelligenceService.analyze(imageBytes, contentType)

        if (diResult.tables.isEmpty()) {
            log.warn("Azure DI 결과에 테이블이 감지되지 않음")
            throw DocumentAnalysisException("이미지에서 표 구조를 감지하지 못했습니다. 이미지 품질을 확인해 주세요.")
        }

        val mainTable = diResult.tables.maxByOrNull { it.cells.size }!!
        log.info("테이블 감지 - 행: ${mainTable.rowCount}, 열: ${mainTable.columnCount}, 셀: ${mainTable.cells.size}")

        val rowMap: Map<Int, Map<Int, String>> = mainTable.cells
            .groupBy { it.rowIndex }
            .mapValues { (_, cells) -> cells.associate { it.columnIndex to it.content } }

        val lastColIndex = mainTable.columnCount - 1
        val startRow = if (skipHeader) 1 else 0

        val rows = rowMap.entries
            .filter { (rowIndex, _) -> rowIndex >= startRow }
            .sortedBy { (rowIndex, _) -> rowIndex }
            .map { (rowIndex, colMap) ->
                val rawLast = colMap[lastColIndex]?.trim() ?: ""
                TableRowResult(
                    rowIndex = rowIndex,
                    firstColumn = colMap[0]?.trim() ?: "",
                    lastColumn = cleanLastColumnValue(rawLast),
                    rawLastColumn = rawLast,
                )
            }
            .filter { it.firstColumn.isNotBlank() }

        val cleanCount = rows.count { it.lastColumn.isNotBlank() }
        log.info("추출 완료 - 총 ${rows.size}개 행, 정상값 ${cleanCount}개, 워터마크 제거 ${rows.size - cleanCount}개")

        return TableExtractionResponse(
            totalRows = rows.size,
            rows = rows,
        )
    }

    /**
     * 워터마크가 섞인 마지막 컬럼 값을 정제합니다.
     *
     * 처리 규칙:
     *   1. 순수 숫자(예: "34") → 그대로 반환
     *   2. 단일 알파벳 대문자(예: "X", "P") → 그대로 반환
     *   3. 공백 분리 토큰 중 순수 숫자가 있으면 마지막 숫자 토큰 반환 (예: "16A 52" → "52")
     *   4. 그 외 워터마크로 판단 → 빈 문자열 반환 (예: "-76A03", "=76Ag", "//")
     */
    private fun cleanLastColumnValue(raw: String): String {
        if (raw.isBlank()) return ""

        // 규칙 1: 순수 숫자
        if (raw.matches(pureNumber)) return raw

        // 규칙 2: 단일 알파벳 대문자 (X, P 같은 유효 기호)
        if (raw.matches(singleLetter)) return raw

        // 규칙 3: 공백으로 분리 후 순수 숫자 토큰 탐색
        val numericTokens = raw.split(Regex("\\s+")).filter { it.matches(pureNumber) }
        if (numericTokens.isNotEmpty()) {
            // 여러 숫자 토큰이 있으면 자릿수가 가장 긴 것 우선, 동일하면 마지막 것
            return numericTokens.maxByOrNull { it.length }!!
        }

        // 규칙 4: 워터마크로 판단
        log.debug("워터마크 감지로 값 제거: \"$raw\"")
        return ""
    }
}
