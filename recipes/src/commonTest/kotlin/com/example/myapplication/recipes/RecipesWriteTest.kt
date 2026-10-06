package com.example.myapplication.recipes

import com.example.myapplication.recipes.TestFixtures.bodyOf
import com.example.myapplication.recipes.TestFixtures.engine
import com.example.myapplication.recipes.TestFixtures.failure
import com.example.myapplication.recipes.TestFixtures.jsonEngine
import com.example.myapplication.recipes.TestFixtures.repository
import com.example.myapplication.recipes.TestFixtures.value
import com.example.myapplication.recipes.model.NewRecipe
import com.example.myapplication.recipes.model.RecipeUpdate
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Write-path and failure-handling tests for [RecipesRepository]. */
internal class RecipesWriteTest : RecipesTestBase() {

    @Test
    fun addRecipePostsJsonBody() = recipeTest {
        val engine = jsonEngine()
        val repository = repository(engine)

        val created = repository.addRecipe(
            NewRecipe(
                name = "Tasty Pizza",
                ingredients = listOf("Dough", "Cheese"),
                servings = 2,
            ),
        ).value()

        assertEquals(1, created.id)
        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/recipes/add", request.url.encodedPath)
        assertEquals("application/json", request.body.contentType?.withoutParameters().toString())

        val body = bodyOf(request)
        assertContains(body, "\"name\":\"Tasty Pizza\"")
        assertContains(body, "\"servings\":2")
        assertContains(body, "\"ingredients\":[\"Dough\",\"Cheese\"]")
        // Unset optional fields must be omitted rather than sent as null.
        assertFalse(body.contains("difficulty"), "Unset fields should not be serialized: $body")
    }

    @Test
    fun addRecipeRejectsBlankNameWithoutCallingTheApi() = recipeTest {
        val engine = jsonEngine()

        val failure = repository(engine).addRecipe(NewRecipe(name = "  ")).failure()

        assertEquals(RecipesFailureKind.InvalidRequest, failure.kind)
        assertTrue(engine.requestHistory.isEmpty())
    }

    @Test
    fun updateRecipeUsesPutAndSendsOnlyChangedFields() = recipeTest {
        val engine = jsonEngine()
        val repository = repository(engine)

        repository.updateRecipe(1, RecipeUpdate(name = "Tasty Pizza")).value()

        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Put, request.method)
        assertEquals("/recipes/1", request.url.encodedPath)
        assertEquals("""{"name":"Tasty Pizza"}""", bodyOf(request))
    }

    @Test
    fun patchRecipeUsesPatch() = recipeTest {
        val engine = jsonEngine()
        val repository = repository(engine)

        repository.patchRecipe(7, RecipeUpdate(cuisine = "Italian")).value()

        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals("/recipes/7", request.url.encodedPath)
        assertEquals("""{"cuisine":"Italian"}""", bodyOf(request))
    }

    @Test
    fun renameRecipePatchesNameOnly() = recipeTest {
        val engine = jsonEngine()

        repository(engine).renameRecipe(3, "New Name").value()

        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Patch, request.method)
        assertEquals("/recipes/3", request.url.encodedPath)
        assertEquals("""{"name":"New Name"}""", bodyOf(request))
    }

    @Test
    fun emptyUpdateIsRejectedWithoutCallingTheApi() = recipeTest {
        val engine = jsonEngine()

        val failure = repository(engine).updateRecipe(1, RecipeUpdate()).failure()

        assertEquals(RecipesFailureKind.InvalidRequest, failure.kind)
        assertTrue(engine.requestHistory.isEmpty())
    }

    @Test
    fun deleteRecipeParsesDeletionMetadata() = recipeTest {
        val engine = jsonEngine(body = TestFixtures.DeletedJson)
        val repository = repository(engine)

        val deleted = repository.deleteRecipe(1).value()

        assertEquals(1, deleted.id)
        assertTrue(deleted.isDeleted)
        assertEquals("2026-01-01T10:00:00.000Z", deleted.deletedOn)

        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/recipes/1", request.url.encodedPath)
    }

    @Test
    fun malformedPayloadBecomesSerializationFailure() = recipeTest {
        val engine = jsonEngine(body = """{"recipes": "not-an-array"}""")
        val repository = repository(engine)

        val failure = repository.getRecipes().failure()

        assertIs<RecipesSerializationFailure>(failure)
        assertEquals(RecipesFailureKind.Serialization, failure.kind)
    }

    @Test
    fun notFoundIsNotRetried() = recipeTest {
        var calls = 0
        val engine = engine { _ ->
            calls += 1
            respond(
                content = TestFixtures.ErrorJson,
                status = HttpStatusCode.NotFound,
                headers = headersOf("Content-Type", "application/json"),
            )
        }
        val repository = repository(
            engine,
            RecipesConfig(retryPolicy = RetryPolicy(maxAttempts = 3, initialDelayMillis = 10, jitterRatio = 0.0)),
        )

        val failure = repository.getRecipe(999).failure()

        assertIs<RecipesHttpFailure>(failure)
        assertEquals(1, calls, "4xx responses must not be retried")
    }

    @Test
    fun serverErrorIsRetriedUntilItSucceeds() = recipeTest {
        var calls = 0
        val engine = engine { _ ->
            calls += 1
            if (calls < 3) {
                respond(
                    content = "{}",
                    status = HttpStatusCode.InternalServerError,
                    headers = headersOf("Content-Type", "application/json"),
                )
            } else {
                respond(
                    content = TestFixtures.SingleRecipeJson,
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "application/json"),
                )
            }
        }
        val repository = repository(
            engine,
            RecipesConfig(
                retryPolicy = RetryPolicy(
                    maxAttempts = 3,
                    initialDelayMillis = 50,
                    maxDelayMillis = 100,
                    jitterRatio = 0.0,
                ),
            ),
        )

        val recipe = repository.getRecipe(1).value()

        assertEquals("Classic Margherita Pizza", recipe.name)
        assertEquals(3, calls)
    }

    @Test
    fun serverErrorIsReportedAfterRetriesAreExhausted() = recipeTest {
        var calls = 0
        val engine = engine { _ ->
            calls += 1
            respond(
                content = "",
                status = HttpStatusCode.ServiceUnavailable,
                headers = headersOf("Content-Type", "application/json"),
            )
        }
        val repository = repository(
            engine,
            RecipesConfig(
                retryPolicy = RetryPolicy(
                    maxAttempts = 3,
                    initialDelayMillis = 50,
                    maxDelayMillis = 100,
                    jitterRatio = 0.0,
                ),
            ),
        )

        val failure = repository.getRecipes().failure()

        assertIs<RecipesHttpFailure>(failure)
        assertEquals(503, failure.statusCode)
        assertTrue(failure.isServerError)
        assertEquals(3, calls)
    }

    @Test
    fun timeoutFailureIsReportedAsTimeoutKind() = recipeTest {
        // MockEngine completes instantly, so an engine level timeout cannot be
        // provoked reliably in a unit test. The mapping from Ktor's timeout
        // exception to `RecipesFailureKind.Timeout` is what matters here.
        val engine = engine { _ ->
            throw HttpRequestTimeoutException("https://dummyjson.com/recipes/1", 30L)
        }
        val repository = repository(
            engine,
            RecipesConfig(timeoutMillis = 30, retryPolicy = RetryPolicy.None),
        )

        val failure = repository.getRecipe(1).failure()

        assertIs<RecipesTimeoutFailure>(failure)
        assertEquals(RecipesFailureKind.Timeout, failure.kind)
        assertContains(failure.message.orEmpty(), "30")
    }

    @Test
    fun resultHelpersExposeValueAndFailure() = recipeTest {
        val success = repository(jsonEngine()).getRecipe(1)
        assertTrue(success.isSuccess)
        assertFalse(success.isFailure)
        assertEquals(1, success.getOrNull()?.id)
        assertEquals(4, success.getOrNull()?.servings)
        assertEquals(1, success.map { it.id }.getOrThrow())
        assertEquals(1, success.getOrElse { error("unreachable") }.id)
        assertEquals("Easy", success.onSuccess { }.getOrNull()?.difficulty)

        val failed = repository(jsonEngine(status = HttpStatusCode.BadGateway, body = "")).getRecipe(1)
        assertTrue(failed.isFailure)
        assertEquals(null, failed.getOrNull())
        assertIs<RecipesHttpFailure>(failed.failureOrNull())
        assertEquals("http", failed.failureOrNull()?.kind)
    }

    @Test
    fun retryPolicyJitterStaysWithinBounds() {
        val policy = RetryPolicy(
            maxAttempts = 5,
            initialDelayMillis = 100,
            maxDelayMillis = 400,
            backoffMultiplier = 2.0,
            jitterRatio = 0.2,
        )

        repeat(50) {
            for (attempt in 1..4) {
                val delay = policy.delayFor(attempt)
                assertTrue(delay in 0..400, "Delay $delay must stay within the configured cap")
            }
        }
    }
}
