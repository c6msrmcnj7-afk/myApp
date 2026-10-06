package com.example.myapplication.recipes

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import kotlin.math.pow
import kotlin.random.Random

/**
 * Immutable configuration of [RecipesRepository].
 *
 * ```kotlin
 * val repository = RecipesRepository.create(
 *     RecipesConfig(baseUrl = "https://dummyjson.com", timeoutMillis = 15_000)
 * )
 * ```
 *
 * @param baseUrl scheme, host and (optionally) path prefix of the API.
 * @param timeoutMillis per-request timeout in milliseconds.
 * @param enableLogging when `true` request/response lines are logged through
 *   the platform logger. Keep it disabled in release builds.
 * @param retryPolicy how transient failures are retried.
 * @param httpClient a caller supplied engine. When provided it is owned by the
 *   caller and is **not** closed by [RecipesRepository.close].
 */
public data class RecipesConfig(
    val baseUrl: String = RecipesApi.BaseUrl,
    val timeoutMillis: Long = 15_000L,
    val enableLogging: Boolean = false,
    val retryPolicy: RetryPolicy = RetryPolicy.Default,
    val httpClient: HttpClient? = null,
) {
    init {
        require(baseUrl.isNotBlank()) { "baseUrl must not be blank" }
        require(timeoutMillis > 0) { "timeoutMillis must be > 0 but was $timeoutMillis" }
    }

    /** [baseUrl] without a trailing slash, so paths can be appended safely. */
    public val normalizedBaseUrl: String
        get() = baseUrl.trimEnd('/')

    public companion object {
        /**
         * Returns the default configuration.
         *
         * Provided because Kotlin default arguments are not exported to
         * Objective-C/Swift.
         */
        public fun default(): RecipesConfig = RecipesConfig()

        /** Returns a configuration pointing at [baseUrl]. */
        public fun forBaseUrl(baseUrl: String): RecipesConfig = RecipesConfig(baseUrl = baseUrl)
    }
}

/**
 * Retry behaviour for transient failures.
 *
 * A failure is transient when it is a [RecipesNetworkFailure], a
 * [RecipesTimeoutFailure], an HTTP 429, or any HTTP 5xx response.
 *
 * @param maxAttempts total number of attempts, including the first one. `1`
 *   disables retrying.
 * @param initialDelayMillis delay before the second attempt.
 * @param maxDelayMillis upper bound of the exponential backoff delay.
 * @param backoffMultiplier factor applied to the delay after every attempt.
 * @param jitterRatio random jitter applied to the delay, `0.0` .. `1.0`.
 */
public data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMillis: Long = 300L,
    val maxDelayMillis: Long = 4_000L,
    val backoffMultiplier: Double = 2.0,
    val jitterRatio: Double = 0.2,
) {
    init {
        require(maxAttempts >= 1) { "maxAttempts must be >= 1 but was $maxAttempts" }
        require(initialDelayMillis >= 0) { "initialDelayMillis must be >= 0" }
        require(maxDelayMillis >= initialDelayMillis) { "maxDelayMillis must be >= initialDelayMillis" }
        require(backoffMultiplier >= 1.0) { "backoffMultiplier must be >= 1.0" }
        require(jitterRatio in 0.0..1.0) { "jitterRatio must be in 0.0..1.0" }
    }

    public companion object {
        /** Three attempts with exponential backoff and 20% jitter. */
        public val Default: RetryPolicy = RetryPolicy()

        /** No retrying: a single attempt per call. */
        public val None: RetryPolicy = RetryPolicy(maxAttempts = 1)
    }

    /**
     * Backoff delay before attempt number [attempt] (1-based), applying
     * [backoffMultiplier] and [jitterRatio].
     *
     * The result never exceeds [maxDelayMillis]: jitter only ever shortens the
     * delay, so a caller can rely on the cap as a hard upper bound.
     */
    public fun delayFor(attempt: Int): Long {
        val exponential = initialDelayMillis * backoffMultiplier.pow((attempt - 1).toDouble())
        val capped = exponential.coerceAtMost(maxDelayMillis.toDouble())
        if (jitterRatio <= 0.0) return capped.toLong()
        return (capped * (1.0 - jitterRatio * Random.nextDouble())).toLong().coerceAtLeast(0L)
    }
}

/** Shared [Json] instance: tolerant of new server fields, ignores nulls. */
internal val RecipesJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    encodeDefaults = false
}
