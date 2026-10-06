package com.example.myapplication.recipes

/**
 * Outcome of a recipes call: either [Success] with a value, or [Failure].
 *
 * ```kotlin
 * when (val result = repository.getRecipe(1)) {
 *     is RecipesResult.Success -> render(result.value)
 *     is RecipesResult.Failure -> showError(result.failure)
 * }
 * ```
 *
 * The type is deliberately not a `kotlin.Result` alias so that it stays usable
 * from Swift, Kotlin/JS and Java without extra bridging.
 */
public sealed class RecipesResult<out T> {

    /** The call succeeded and produced [value]. */
    public data class Success<out T>(val value: T) : RecipesResult<T>()

    /** The call failed with [failure]. */
    public data class Failure(val failure: RecipesFailure) : RecipesResult<Nothing>()

    /** The value when this is a [Success], otherwise `null`. */
    public fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    /** The failure when this is a [Failure], otherwise `null`. */
    public fun failureOrNull(): RecipesFailure? = when (this) {
        is Success -> null
        is Failure -> failure
    }

    /** `true` when the call succeeded. */
    public val isSuccess: Boolean get() = this is Success

    /** `true` when the call failed. */
    public val isFailure: Boolean get() = this is Failure

    /** Returns the value or throws the wrapped [RecipesFailure]. */
    public fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw failure
    }

    /** Returns the value, or the result of [fallback] when the call failed. */
    public inline fun getOrElse(fallback: () -> @UnsafeVariance T): T = when (this) {
        is Success -> value
        is Failure -> fallback()
    }

    /** Transforms a successful value, leaving failures untouched. */
    public inline fun <R> map(transform: (T) -> R): RecipesResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    /** Runs [action] when the call succeeded. */
    public inline fun onSuccess(action: (T) -> Unit): RecipesResult<T> {
        if (this is Success) action(value)
        return this
    }

    /** Runs [action] when the call failed. */
    public inline fun onFailure(action: (RecipesFailure) -> Unit): RecipesResult<T> {
        if (this is Failure) action(failure)
        return this
    }

    public companion object {
        /** Wraps [value] in a [Success]. */
        public fun <T> success(value: T): RecipesResult<T> = Success(value)

        /** Wraps [failure] in a [Failure]. */
        public fun <T> failure(failure: RecipesFailure): RecipesResult<T> = Failure(failure)
    }
}
