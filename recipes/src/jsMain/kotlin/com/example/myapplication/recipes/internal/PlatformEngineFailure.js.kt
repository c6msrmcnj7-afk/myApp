package com.example.myapplication.recipes.internal

/**
 * The browser `fetch` engine surfaces failures as `TypeError`/`DOMException`,
 * which the JavaScript source set does not need to translate: the repository
 * wraps them as unexpected failures.
 */
internal actual fun platformEngineFailure(error: Throwable): Throwable? = null
