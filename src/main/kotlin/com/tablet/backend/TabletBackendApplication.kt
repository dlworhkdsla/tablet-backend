package com.tablet.backend

import com.tablet.backend.config.JwtProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties::class)
class TabletBackendApplication

fun main(args: Array<String>) {
    runApplication<TabletBackendApplication>(*args)
}
