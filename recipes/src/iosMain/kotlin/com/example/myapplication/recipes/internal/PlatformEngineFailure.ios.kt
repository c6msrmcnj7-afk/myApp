package com.example.myapplication.recipes.internal

import com.example.myapplication.recipes.RecipesNetworkFailure
import io.ktor.client.engine.darwin.DarwinHttpRequestException
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURLErrorCancelled
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorNotConnectedToInternet
import platform.Foundation.NSURLErrorTimedOut
import kotlin.experimental.ExperimentalNativeApi

/**
 * Classifies NSURLSession failures reported by the Darwin engine.
 *
 * The Darwin engine throws [DarwinHttpRequestException] for transport level
 * errors instead of a portable `IOException`, so the reason is read from the
 * wrapped `NSError`.
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
internal actual fun platformEngineFailure(error: Throwable): Throwable? {
    if (error !is DarwinHttpRequestException) return null

    val origin = error.origin
    val isNetworkReason = origin.domain == NSURLErrorDomain &&
        origin.code in TRANSPORT_ERROR_CODES

    return if (isNetworkReason) {
        RecipesNetworkFailure(
            message = "The recipes request could not reach the server: $origin",
            cause = error,
        )
    } else {
        null
    }
}

private val TRANSPORT_ERROR_CODES: Set<Long> = setOf(
    NSURLErrorNotConnectedToInternet,
    NSURLErrorTimedOut,
    NSURLErrorCancelled,
)
