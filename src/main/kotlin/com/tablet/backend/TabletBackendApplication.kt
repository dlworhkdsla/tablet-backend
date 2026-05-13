package com.tablet.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration
import org.springframework.boot.autoconfigure.r2dbc.R2dbcTransactionManagerAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(
    exclude = [
        R2dbcAutoConfiguration::class,
        R2dbcTransactionManagerAutoConfiguration::class,
        R2dbcDataAutoConfiguration::class,
        R2dbcRepositoriesAutoConfiguration::class,
    ],
)
class TabletBackendApplication

fun main(args: Array<String>) {
    runApplication<TabletBackendApplication>(*args)
}