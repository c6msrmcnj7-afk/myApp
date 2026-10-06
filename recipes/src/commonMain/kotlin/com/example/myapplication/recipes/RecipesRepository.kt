package com.example.myapplication.recipes

import com.example.myapplication.recipes.internal.RecipesApiClient
import com.example.myapplication.recipes.internal.RecipesApiClientImpl
import com.example.myapplication.recipes.internal.mapEngineFailure
import com.example.myapplication.recipes.internal.unexpectedFailure
import com.example.myapplication.recipes.internal.withRetry
import com.example.myapplication.recipes.model.DeletedRecipe
import com.example.myapplication.recipes.model.NewRecipe
import com.example.myapplication.recipes.model.Page
import com.example.myapplication.recipes.model.Recipe
import com.example.myapplication.recipes.model.RecipeUpdate
import com.example.myapplication.recipes.model.RecipesQuery
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Entry point of the library: a suspend, error-safe client for the
 * [DummyJSON recipes API](https://dummyjson.com/docs/recipes).
 *
 * ```kotlin
 * val repository = RecipesRepository.create()
 * when (val result = repository.getRecipes()) {
 *     is RecipesResult.Success -> result.value.items.forEach(::println)
 *     is RecipesResult.Failure -> println(result.failure.message)
 * }
 * ```
 *
 * The instance is safe to share between coroutines; call [close] when the
 * owning scope ends.
 *
 * @param config request configuration; pass a custom [RecipesConfig.httpClient]
 *   to inject your own engine (for example a mock engine in tests).
 */
public class RecipesRepository internal constructor(
    public val config: RecipesConfig,
) {
    private val ownsHttpClient: Boolean = config.httpClient == null
    private val httpClient: HttpClient = config.httpClient ?: buildRecipesHttpClient(config)
    private val api: RecipesApiClient = RecipesApiClientImpl(config, httpClient, RecipesJson)
    private val closed = Mutex()
    private var isClosed: Boolean = false

    // ------------------------------------------------------------------
    // Reads
    // ------------------------------------------------------------------

    /** Returns one page of recipes. */
    public suspend fun getRecipes(query: RecipesQuery = RecipesQuery()): RecipesResult<Page<Recipe>> =
        execute { api.getRecipes(query) }

    /** Returns the first page of recipes, using the API default page size. */
    public suspend fun getRecipes(limit: Int, skip: Int = 0): RecipesResult<Page<Recipe>> =
        getRecipes(RecipesQuery(limit = limit, skip = skip))

    /** Returns a single recipe by [id]. */
    public suspend fun getRecipe(id: Int): RecipesResult<Recipe> = execute {
        requirePositive(id, "id")
        api.getRecipe(id)
    }

    /** Returns the page of recipes whose name matches [query]. */
    public suspend fun searchRecipes(
        query: String,
        page: RecipesQuery = RecipesQuery(),
    ): RecipesResult<Page<Recipe>> = execute { api.searchRecipes(query, page) }

    /** Returns the page of recipes carrying the given [tag]. */
    public suspend fun getRecipesByTag(
        tag: String,
        page: RecipesQuery = RecipesQuery(),
    ): RecipesResult<Page<Recipe>> = execute { api.getRecipesByTag(tag, page) }

    /** Returns the page of recipes belonging to the given [mealType], e.g. `"Dinner"`. */
    public suspend fun getRecipesByMealType(
        mealType: String,
        page: RecipesQuery = RecipesQuery(),
    ): RecipesResult<Page<Recipe>> = execute { api.getRecipesByMealType(mealType, page) }

    /** Returns every tag known to the API. */
    public suspend fun getTags(): RecipesResult<List<String>> = execute { api.getTags() }

    /**
     * Returns the page of recipes matching [difficulty], filtered on the client
     * because the API does not expose a difficulty endpoint.
     *
     * @param query used for paging; [RecipesQuery.limit] and [RecipesQuery.skip]
     *   are applied to the API page that is filtered afterwards.
     */
    public suspend fun getRecipesByDifficulty(
        difficulty: String,
        query: RecipesQuery = RecipesQuery(),
    ): RecipesResult<Page<Recipe>> = getRecipes(query).map { page ->
        val matches = page.items.filter { it.difficulty.equals(difficulty, ignoreCase = true) }
        page.copy(items = matches, total = matches.size)
    }

    /** Returns the page of recipes matching [cuisine], filtered on the client. */
    public suspend fun getRecipesByCuisine(
        cuisine: String,
        query: RecipesQuery = RecipesQuery(),
    ): RecipesResult<Page<Recipe>> = getRecipes(query).map { page ->
        val matches = page.items.filter { it.cuisine.equals(cuisine, ignoreCase = true) }
        page.copy(items = matches, total = matches.size)
    }

    /**
     * Fetches every page of recipes and returns them as a single list.
     *
     * @param pageSize number of recipes requested per round trip.
     * @param maxRecipes safety cap so a misbehaving server cannot loop forever.
     */
    public suspend fun getAllRecipes(
        pageSize: Int = RecipesApi.DefaultLimit,
        maxRecipes: Int = 1_000,
    ): RecipesResult<List<Recipe>> {
        val collected = mutableListOf<Recipe>()
        var query = RecipesQuery(limit = pageSize, skip = 0)
        while (true) {
            val page = when (val result = getRecipes(query)) {
                is RecipesResult.Success -> result.value
                is RecipesResult.Failure -> return RecipesResult.Failure(result.failure)
            }
            collected += page.items
            val next = page.nextSkip()
            if (next == null || page.isEmpty || collected.size >= maxRecipes) {
                return RecipesResult.Success(collected.take(maxRecipes))
            }
            query = query.copy(skip = next)
        }
    }

    // ------------------------------------------------------------------
    // Writes (simulated by DummyJSON, nothing is persisted server side)
    // ------------------------------------------------------------------

    /** Simulates `POST /recipes/add` and returns the recipe the API echoed back. */
    public suspend fun addRecipe(recipe: NewRecipe): RecipesResult<Recipe> = execute { api.addRecipe(recipe) }

    /** Simulates `PUT /recipes/{id}`, replacing the fields carried by [update]. */
    public suspend fun updateRecipe(id: Int, update: RecipeUpdate): RecipesResult<Recipe> = execute {
        requirePositive(id, "id")
        api.updateRecipe(id, update, patch = false)
    }

    /** Simulates `PATCH /recipes/{id}`, changing only the fields carried by [update]. */
    public suspend fun patchRecipe(id: Int, update: RecipeUpdate): RecipesResult<Recipe> = execute {
        requirePositive(id, "id")
        api.updateRecipe(id, update, patch = true)
    }

    /** Renames the recipe with [id], keeping every other field untouched. */
    public suspend fun renameRecipe(id: Int, name: String): RecipesResult<Recipe> =
        patchRecipe(id, RecipeUpdate(name = name))

    /** Simulates `DELETE /recipes/{id}`. */
    public suspend fun deleteRecipe(id: Int): RecipesResult<DeletedRecipe> = execute {
        requirePositive(id, "id")
        api.deleteRecipe(id)
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /**
     * Releases the underlying HTTP client.
     *
     * No-op when the client was supplied through [RecipesConfig.httpClient],
     * because the caller owns it in that case. Safe to call more than once.
     */
    public suspend fun close() {
        closed.withLock {
            if (isClosed) return
            isClosed = true
            if (ownsHttpClient) httpClient.close()
        }
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private suspend fun <T> execute(block: suspend () -> T): RecipesResult<T> = try {
        withContext(recipesIoDispatcher) {
            withRetry(config) {
                try {
                    block()
                } catch (error: Throwable) {
                    throw mapEngineFailure(error, config)
                }
            }
        }
        .let { RecipesResult.Success(it) }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        RecipesResult.Failure(error.toRecipesFailure())
    }

    private fun requirePositive(id: Int, name: String) {
        if (id <= 0) {
            throw RecipesInvalidRequestFailure("$name must be > 0 but was $id.")
        }
    }

    public companion object {
        /** Creates a repository with the given [config]. */
        public fun create(config: RecipesConfig = RecipesConfig()): RecipesRepository =
            RecipesRepository(config)

        /**
         * Creates a repository configured for [baseUrl].
         *
         * Kotlin default arguments are not exported to Objective-C/Swift, so
         * this overload gives Swift callers a zero-argument factory.
         */
        public fun create(): RecipesRepository = RecipesRepository(RecipesConfig())

        /** Creates a repository that talks to [baseUrl] instead of DummyJSON. */
        public fun create(baseUrl: String): RecipesRepository =
            RecipesRepository(RecipesConfig(baseUrl = baseUrl))
    }
}

/**
 * Converts any throwable raised by the client layer into a [RecipesFailure].
 *
 * [ResponseException] is handled once because it is the common supertype of
 * both [ClientRequestException] (4xx) and [ServerResponseException] (5xx).
 */
internal fun Throwable.toRecipesFailure(): RecipesFailure = when (this) {
    is RecipesFailure -> this
    is HttpRequestTimeoutException -> RecipesTimeoutFailure(
        message = message ?: "The recipes request timed out.",
        cause = this,
    )
    is ResponseException -> RecipesHttpFailure(
        statusCode = response.status.value,
        message = message ?: "The recipes request failed with HTTP ${response.status.value}.",
        cause = this,
    )
    is CancellationException -> RecipesCancellationFailure(
        message = message ?: "The recipes request was cancelled.",
        cause = this,
    )
    else -> unexpectedFailure(this)
}
