package com.example.myapplication.recipes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Base class for repository tests.
 *
 * The repository runs blocking work on [recipesIoDispatcher], which is
 * `Dispatchers.IO` on JVM/Android. Installing a [StandardTestDispatcher] as the
 * main dispatcher makes that borrow `runTest`'s scheduler, so retry backoff is
 * driven by virtual time instead of real delays.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class RecipesTestBase {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun installTestDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun removeTestDispatcher() {
        Dispatchers.resetMain()
    }

    /** Runs [body] in a test coroutine using the installed dispatcher. */
    protected fun recipeTest(body: suspend () -> Unit): TestResult = runTest(testDispatcher) { body() }
}
