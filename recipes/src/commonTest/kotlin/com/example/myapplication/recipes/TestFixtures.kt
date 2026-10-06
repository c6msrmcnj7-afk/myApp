package com.example.myapplication.recipes

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

/**
 * Shared fixtures for the recipes tests: sample DummyJSON payloads and a
 * [MockEngine] backed repository.
 */
internal object TestFixtures {

    val SingleRecipeJson: String = """
        {
          "id": 1,
          "name": "Classic Margherita Pizza",
          "ingredients": ["Pizza dough", "Tomato sauce", "Fresh mozzarella cheese"],
          "instructions": ["Preheat the oven to 475 F.", "Bake for 12-15 minutes."],
          "prepTimeMinutes": 20,
          "cookTimeMinutes": 15,
          "servings": 4,
          "difficulty": "Easy",
          "cuisine": "Italian",
          "caloriesPerServing": 300,
          "tags": ["Pizza", "Italian"],
          "userId": 45,
          "image": "https://cdn.dummyjson.com/recipe-images/1.webp",
          "rating": 4.6,
          "reviewCount": 3,
          "mealType": ["Dinner"]
        }
    """.trimIndent()

    /** Two recipes: one complete and one partial, as `select` can produce. */
    val PageJson: String = """
        {
          "recipes": [
            {
              "id": 1,
              "name": "Classic Margherita Pizza",
              "ingredients": ["Pizza dough"],
              "instructions": ["Bake."],
              "prepTimeMinutes": 20,
              "cookTimeMinutes": 15,
              "servings": 4,
              "difficulty": "Easy",
              "cuisine": "Italian",
              "caloriesPerServing": 300,
              "tags": ["Pizza"],
              "userId": 45,
              "image": "https://cdn.dummyjson.com/recipe-images/1.webp",
              "rating": 4.6,
              "reviewCount": 3,
              "mealType": ["Dinner"]
            },
            { "id": 2, "name": "Chicken Biryani", "image": "https://cdn.dummyjson.com/recipe-images/2.webp" }
          ],
          "total": 50,
          "skip": 10,
          "limit": 2
        }
    """.trimIndent()

    const val TagsJson: String = """["Pizza", "Italian", "Vegetarian"]"""

    val DeletedJson: String = """
        {
          "id": 1,
          "name": "Classic Margherita Pizza",
          "isDeleted": true,
          "deletedOn": "2026-01-01T10:00:00.000Z"
        }
    """.trimIndent()

    const val ErrorJson: String = """{"message":"Recipe with id '999' not found"}"""

    /** Builds a [MockEngine] that always answers with [body] and [status]. */
    fun jsonEngine(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = SingleRecipeJson,
    ): MockEngine = MockEngine { _ ->
        respond(
            content = body,
            status = status,
            headers = headersOf("Content-Type", "application/json"),
        )
    }

    /** Builds a [MockEngine] driven by [handler]. */
    fun engine(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): MockEngine = MockEngine(handler)

    /**
     * Wraps [engine] in a repository.
     *
     * @param config applied on top of the engine; by default retries are
     *   disabled so each test observes exactly the requests it sets up.
     */
    fun repository(
        engine: MockEngine,
        config: RecipesConfig = RecipesConfig(retryPolicy = RetryPolicy.None),
    ): RecipesRepository = RecipesRepository.create(
        config.copy(httpClient = HttpClient(engine)),
    )

    /** Body of [request] decoded as UTF-8 text. */
    suspend fun bodyOf(request: HttpRequestData): String =
        request.body.toByteArray().decodeToString()

    /** Unwraps a successful result or fails the test. */
    fun <T> RecipesResult<T>.value(): T = when (this) {
        is RecipesResult.Success -> value
        is RecipesResult.Failure -> error("Expected success but got ${failure::class.simpleName}: ${failure.message}")
    }

    /** Unwraps a failure or fails the test. */
    fun <T> RecipesResult<T>.failure(): RecipesFailure = when (this) {
        is RecipesResult.Failure -> failure
        is RecipesResult.Success -> error("Expected a failure but got $value")
    }
}
