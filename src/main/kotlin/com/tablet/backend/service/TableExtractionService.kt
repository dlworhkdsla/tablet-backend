package com.tablet.backend.service

import com.tablet.backend.dto.document.DiCell
import com.tablet.backend.dto.document.DiWord
import com.tablet.backend.dto.document.TableExtractionResponse
import com.tablet.backend.dto.document.TableRowResult
import com.tablet.backend.exception.DocumentAnalysisException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.abs
import kotlin.math.atan2

@Service
class TableExtractionService(
    private val documentIntelligenceService: DocumentIntelligenceService,
) {
    private val log = LoggerFactory.getLogger(TableExtractionService::class.java)

    companion object {
        // 이 각도 이상으로 기울어진 단어는 워터마크로 판단
        private const val WATERMARK_ANGLE_THRESHOLD = 15.0
    }

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

        // 전체 페이지에서 단어 목록 수집
        val allWords = diResult.pages.flatMap { it.words }

        // 기울기 > WATERMARK_ANGLE_THRESHOLD 인 단어의 span offset → 워터마크 집합
        val watermarkOffsets = allWords
            .filter { word ->
                word.polygon.size >= 4 &&
                    abs(calculateRotationAngle(word.polygon)) > WATERMARK_ANGLE_THRESHOLD
            }
            .map { it.span.offset }
            .toSet()

        log.info("워터마크 단어 감지: ${watermarkOffsets.size}개 (기울기 > ${WATERMARK_ANGLE_THRESHOLD}°)")

        val mainTable = diResult.tables.maxByOrNull { it.cells.size }!!
        log.info("테이블 감지 - 행: ${mainTable.rowCount}, 열: ${mainTable.columnCount}")

        val lastColIndex = mainTable.columnCount - 1
        val startRow = if (skipHeader) 1 else 0

        val rowMap = mainTable.cells
            .groupBy { it.rowIndex }
            .mapValues { (_, cells) ->
                cells.associate { cell ->
                    cell.columnIndex to getCleanCellContent(cell, allWords, watermarkOffsets)
                }
            }

        val rows = rowMap.entries
            .filter { (rowIndex, _) -> rowIndex >= startRow }
            .sortedBy { (rowIndex, _) -> rowIndex }
            .map { (rowIndex, colMap) ->
                val rawLast = colMap[lastColIndex] ?: ""
                TableRowResult(
                    rowIndex = rowIndex,
                    firstColumn = colMap[0] ?: "",
                    lastColumn = rawLast,
                    rawLastColumn = mainTable.cells
                        .find { it.rowIndex == rowIndex && it.columnIndex == lastColIndex }
                        ?.content?.trim() ?: "",
                )
            }
            .filter { it.firstColumn.isNotBlank() }

        val cleanCount = rows.count { it.lastColumn.isNotBlank() }
        log.info(
            "추출 완료 - 총 ${rows.size}개 행, " +
                "정상값 ${cleanCount}개, " +
                "워터마크 제거 ${rows.size - cleanCount}개",
        )

        return TableExtractionResponse(
            totalRows = rows.size,
            rows = rows,
        )
    }

    /**
     * 셀에 속한 단어 중 워터마크(기울어진 단어)를 제외한 텍스트를 재조합합니다.
     *
     * 1. 셀의 spans로 해당 셀에 속하는 단어를 특정
     * 2. watermarkOffsets에 포함된 단어(기울기 초과) 제거
     * 3. 남은 단어를 span.offset 순서로 정렬하여 재조합
     */
    private fun getCleanCellContent(
        cell: DiCell,
        allWords: List<DiWord>,
        watermarkOffsets: Set<Int>,
    ): String {
        // spans 정보가 없으면 기존 content에서 텍스트 기반 정제
        if (cell.spans.isEmpty()) {
            return cleanByTextRule(cell.content.trim())
        }

        val cleanWords = allWords
            .filter { word ->
                // 워터마크 단어 제외
                word.span.offset !in watermarkOffsets &&
                    // 이 셀의 span 범위 안에 포함된 단어만 선택
                    cell.spans.any { span ->
                        word.span.offset >= span.offset &&
                            word.span.offset + word.span.length <= span.offset + span.length
                    }
            }
            .sortedBy { it.span.offset }

        return cleanWords.joinToString(" ") { it.content }.trim()
    }

    /**
     * polygon 배열로 단어의 기울기 각도(도)를 계산합니다.
     *
     * polygon = [x0,y0, x1,y1, x2,y2, x3,y3] (시계방향 4꼭짓점)
     * 상단 좌측(x0,y0) → 상단 우측(x1,y1) 벡터의 각도
     * 정상 텍스트: ≈ 0°, 30° 기울어진 워터마크: ≈ ±30°
     */
    private fun calculateRotationAngle(polygon: List<Double>): Double {
        val dx = polygon[2] - polygon[0]
        val dy = polygon[3] - polygon[1]
        return Math.toDegrees(atan2(dy, dx))
    }

    /**
     * spans 정보가 없을 때 사용하는 텍스트 기반 정제 (fallback).
     * 알파벳+숫자 혼합 토큰과 특수문자 토큰을 제거합니다.
     */
    private fun cleanByTextRule(raw: String): String {
        if (raw.isBlank()) return ""
        val validTokens = raw.split(Regex("\\s+"))
            .filter { token ->
                val hasLetter = token.any { it.isLetter() }
                val hasDigit  = token.any { it.isDigit() }
                when {
                    hasLetter && hasDigit -> false
                    !hasLetter && !hasDigit -> false
                    else -> true
                }
            }
        if (validTokens.isEmpty()) return ""
        val numericTokens = validTokens.filter { it.matches(pureNumber) }
        return if (numericTokens.isNotEmpty()) numericTokens.last() else validTokens.last()
    }
}
