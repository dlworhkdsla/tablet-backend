package com.tablet.backend.router

import com.tablet.backend.handler.DocumentAnalysisHandler
import com.tablet.backend.handler.TableExtractionHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class DocumentAnalysisRouter(
    private val documentAnalysisHandler: DocumentAnalysisHandler,
    private val tableExtractionHandler: TableExtractionHandler,
) {
    @Bean
    fun documentAnalysisRoutes() = coRouter {
        "/api/v1/documents".nest {
            contentType(MediaType.MULTIPART_FORM_DATA).nest {
                POST("/analyze", documentAnalysisHandler::analyzeDocument)
                POST("/extract-table", tableExtractionHandler::extractTable)
            }
        }
    }
}
