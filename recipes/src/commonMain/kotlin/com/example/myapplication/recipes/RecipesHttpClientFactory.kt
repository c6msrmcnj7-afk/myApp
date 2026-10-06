package com.example.myapplication.recipes

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json

/**
 * Creates the HTTP engine for the current platform.
 *
 * - Android/JVM: `OkHttp`
 * - iOS: `Darwin` (NSURLSession)
 * - JS: `Js` (fetch)
 */
internal expect fun createPlatformHttpClientEngine(): HttpClientEngine

/**
 * Builds a configured [HttpClient] with content negotiation, timeouts and
 * optional logging.
 */
internal fun buildRecipesHttpClient(config: RecipesConfig): HttpClient {
    val engine = config.httpClient?.engine ?: createPlatformHttpClientEngine()
    return HttpClient(engine) {
        expectSuccess = false

        install(ContentNegotiation) {
            json(RecipesJson)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = config.timeoutMillis
            connectTimeoutMillis = config.timeoutMillis
            socketTimeoutMillis = config.timeoutMillis
        }
        if (config.enableLogging) {
            install(Logging) {
                level = LogLevel.INFO
            }
        }
    }
}
