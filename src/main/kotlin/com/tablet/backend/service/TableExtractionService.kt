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

    private val pureNumber = Regex("^\\d+$")

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
     *   - 공백으로 토큰 분리
     *   - 알파벳과 숫자가 동시에 포함된 토큰(혼합 문자) → 워터마크로 판단, 제거
     *   - 특수문자로만 이루어진 토큰(예: "//", "/", "\"") → 제거
     *   - 남은 토큰(순수 숫자, 순수 알파벳)만 유효 데이터
     *   - 유효 토큰이 없으면 빈 문자열 반환
     */
    private fun cleanLastColumnValue(raw: String): String {
        if (raw.isBlank()) return ""

        val validTokens = raw.split(Regex("\\s+"))
            .filter { token ->
                val hasLetter = token.any { it.isLetter() }
                val hasDigit  = token.any { it.isDigit() }
                // 알파벳+숫자 혼합 → 워터마크, 제외
                // 특수문자만 → 의미 없음, 제외
                when {
                    hasLetter && hasDigit -> false   // 혼합 문자 (워터마크)
                    !hasLetter && !hasDigit -> false  // 순수 특수문자
                    else -> true                      // 순수 숫자 또는 순수 알파벳
                }
            }

        if (validTokens.isEmpty()) {
            log.debug("워터마크로 판단하여 값 제거: \"$raw\"")
            return ""
        }

        // 숫자 토큰 우선, 없으면 알파벳 토큰 반환
        val numericTokens = validTokens.filter { it.matches(pureNumber) }
        return if (numericTokens.isNotEmpty()) numericTokens.last() else validTokens.last()
    }
}
