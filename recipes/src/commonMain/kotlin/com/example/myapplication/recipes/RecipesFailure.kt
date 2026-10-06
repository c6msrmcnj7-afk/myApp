package com.example.myapplication.recipes

/**
 * Machine readable classification of a failed recipes call.
 *
 * Every subclass of [RecipesFailure] carries exactly one of these values, so
 * callers can branch on `failure.kind` without string matching.
 */
public object RecipesFailureKind {
    /** No usable network connection, DNS failure, or TLS problem. */
    public const val Network: String = "network"

    /** The request exceeded [RecipesConfig.timeoutMillis]. */
    public const val Timeout: String = "timeout"

    /** The server answered with a 4xx/5xx status code. */
    public const val Http: String = "http"

    /** The response body could not be decoded into a recipe payload. */
    public const val Serialization: String = "serialization"

    /** The caller supplied an invalid argument before any request was made. */
    public const val InvalidRequest: String = "invalid_request"

    /** The client was closed, or the calling coroutine was cancelled. */
    public const val Cancelled: String = "cancelled"

    /** Anything that does not fit the categories above. */
    public const val Unexpected: String = "unexpected"
}

/**
 * Base type of every error surfaced by this library.
 *
 * The recipes API never fails the caller with a raw exception: all entry points
 * return [RecipesResult], which holds either a value or one of these failures.
 */
public sealed class RecipesFailure(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /** One of the constants in [RecipesFailureKind]. */
    public abstract val kind: String
}

/** The request could not reach the server. */
public class RecipesNetworkFailure(
    message: String,
    cause: Throwable? = null,
) : RecipesFailure(message, cause) {
    override val kind: String get() = RecipesFailureKind.Network
}

/** The request took longer than the configured timeout. */
public class RecipesTimeoutFailure(
    message: String,
    cause: Throwable? = null,
) : RecipesFailure(message, cause) {
    override val kind: String get() = RecipesFailureKind.Timeout
}

/** The server answered with an unsuccessful status code. */
public class RecipesHttpFailure(
    /** HTTP status code returned by the server. */
    public val statusCode: Int,
    message: String,
    /** Raw response body, when the server sent one. */
    public val responseBody: String? = null,
    cause: Throwable? = null,
) : RecipesFailure(message, cause) {
    override val kind: String get() = RecipesFailureKind.Http

    /** `true` when the status code is 5xx. */
    public val isServerError: Boolean get() = statusCode in 500..599

    /** `true` when the status code is 429. */
    public val isRateLimited: Boolean get() = statusCode == 429
}

/** The response body did not match the expected recipe schema. */
public class RecipesSerializationFailure(
    message: String,
    cause: Throwable? = null,
) : RecipesFailure(message, cause) {
    override val kind: String get() = RecipesFailureKind.Serialization
}

/** The caller passed an argument the API cannot accept. */
public class RecipesInvalidRequestFailure(
    message: String,
    cause: Throwable? = null,
) : RecipesFailure(message, cause) {
    override val kind: String get() = RecipesFailureKind.InvalidRequest
}

/** The call was cancelled, typically because its scope was cancelled. */
public class RecipesCancellationFailure(
    message: String = "The recipes request was cancelled.",
    cause: Throwable? = null,
) : RecipesFailure(message, cause) {
    override val kind: String get() = RecipesFailureKind.Cancelled
}

/** An error that does not match any other category. */
public class RecipesUnexpectedFailure(
    message: String,
    cause: Throwable? = null,
) : RecipesFailure(message, cause) {
    override val kind: String get() = RecipesFailureKind.Unexpected
}
