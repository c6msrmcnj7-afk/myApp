package com.example.myapplication.recipes.internal

import com.example.myapplication.recipes.RecipesConfig
import com.example.myapplication.recipes.RecipesHttpFailure
import com.example.myapplication.recipes.RecipesNetworkFailure
import com.example.myapplication.recipes.RecipesTimeoutFailure
import com.example.myapplication.recipes.RecipesUnexpectedFailure
import com.example.myapplication.recipes.RetryPolicy
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.io.IOException
import kotlin.math.pow
import kotlin.random.Random

/**
 * Executes an HTTP call, retrying transient failures according to
 * [RecipesConfig.retryPolicy].
 *
 * [block] performs the request and returns the decoded payload; it is
 * re-invoked from scratch on every attempt.
 */
internal suspend fun <T> withRetry(
    config: RecipesConfig,
    block: suspend () -> T,
): T {
    val policy: RetryPolicy = config.retryPolicy
    var attempt = 1
    while (true) {
        try {
            return block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            val canRetry = attempt < policy.maxAttempts && failure.isTransient()
            if (!canRetry) throw failure
            delay(policy.delayFor(attempt))
            attempt++
        }
    }
}

/** `true` for failures that are worth retrying. */
internal fun Throwable.isTransient(): Boolean = when (this) {
    is RecipesNetworkFailure, is RecipesTimeoutFailure -> true
    is RecipesHttpFailure -> isServerError || isRateLimited
    else -> false
}

/**
 * Translates engine-level exceptions into library failures.
 *
 * Anything unrecognised is passed through unchanged so the repository can wrap
 * it as an unexpected failure.
 */
internal fun mapEngineFailure(error: Throwable, config: RecipesConfig): Throwable {
    platformEngineFailure(error)?.let { return it }
    return when (error) {
        is HttpRequestTimeoutException -> RecipesTimeoutFailure(
            message = "The recipes request timed out after ${config.timeoutMillis} ms.",
            cause = error,
        )

        is IOException -> RecipesNetworkFailure(
            message = error.message ?: "The recipes request could not reach the server.",
            cause = error,
        )

        else -> error
    }
}

/**
 * Maps platform specific engine errors, in particular connection problems that
 * are not wrapped in a portable exception.
 *
 * Engines are not uniform here: the Darwin (NSURLSession) engine only reports
 * failures such as an unresolvable host through [platformEngineFailure].
 * Returns `null` when the error is not recognised.
 */
internal expect fun platformEngineFailure(error: Throwable): Throwable?

/** Builds an [RecipesHttpFailure] from an unsuccessful response. */
internal fun httpFailure(response: HttpResponse, body: String?): RecipesHttpFailure {
    val status: HttpStatusCode = response.status
    val detail = body?.takeIf { it.isNotBlank() }?.take(2_000)
    val message = buildString {
        append("Recipes request failed with HTTP ")
        append(status.value)
        append(' ')
        append(status.description)
        if (detail != null) {
            append(": ")
            append(detail)
        }
    }
    return RecipesHttpFailure(
        statusCode = status.value,
        message = message,
        responseBody = detail,
    )
}

/** Wraps an unroutable error so callers still receive a named failure type. */
internal fun unexpectedFailure(error: Throwable): RecipesUnexpectedFailure = RecipesUnexpectedFailure(
    message = error.message ?: "Unexpected error while calling the recipes API.",
    cause = error,
)
