package com.example.myapplication.recipes

import kotlinx.coroutines.CoroutineDispatcher

/**
 * The dispatcher used for blocking HTTP engine work.
 *
 * Kotlin/Native and Kotlin/JS have no `Dispatchers.IO`, so every platform
 * declares its own actual value.
 */
internal expect val recipesIoDispatcher: CoroutineDispatcher
