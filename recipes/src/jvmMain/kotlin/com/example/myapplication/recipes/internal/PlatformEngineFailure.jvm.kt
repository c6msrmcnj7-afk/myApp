package com.example.myapplication.recipes.internal

/**
 * The OkHttp engine reports every transport failure as a `java.io.IOException`,
 * which [mapEngineFailure] already handles.
 */
internal actual fun platformEngineFailure(error: Throwable): Throwable? = null
