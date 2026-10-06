package com.example.myapplication.recipes.internal

import com.example.myapplication.recipes.RecipesApi
import com.example.myapplication.recipes.RecipesConfig
import com.example.myapplication.recipes.RecipesInvalidRequestFailure
import com.example.myapplication.recipes.RecipesSerializationFailure
import com.example.myapplication.recipes.model.DeletedRecipe
import com.example.myapplication.recipes.model.NewRecipe
import com.example.myapplication.recipes.model.Page
import com.example.myapplication.recipes.model.Recipe
import com.example.myapplication.recipes.model.RecipeUpdate
import com.example.myapplication.recipes.model.RecipesQuery
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Thin, engine-agnostic wrapper around the DummyJSON recipes endpoints.
 *
 * Every method performs exactly one HTTP round trip and either returns a
 * decoded payload or throws one of the failures defined in
 * [com.example.myapplication.recipes.RecipesFailure]. Retries live in
 * [com.example.myapplication.recipes.RecipesRepository].
 */
internal interface RecipesApiClient {
    suspend fun getRecipes(query: RecipesQuery): Page<Recipe>
    suspend fun getRecipe(id: Int): Recipe
    suspend fun searchRecipes(query: String, page: RecipesQuery): Page<Recipe>
    suspend fun getRecipesByTag(tag: String, page: RecipesQuery): Page<Recipe>
    suspend fun getRecipesByMealType(mealType: String, page: RecipesQuery): Page<Recipe>
    suspend fun getTags(): List<String>
    suspend fun addRecipe(recipe: NewRecipe): Recipe
    suspend fun updateRecipe(id: Int, update: RecipeUpdate, patch: Boolean): Recipe
    suspend fun deleteRecipe(id: Int): DeletedRecipe
}

/** HTTP verbs supported by the update endpoint. */
internal enum class UpdateVerb(internal val httpMethod: HttpMethod) {
    Put(HttpMethod.Put),
    Patch(HttpMethod.Patch),
}

internal class RecipesApiClientImpl(
    private val config: RecipesConfig,
    private val httpClient: HttpClient,
    private val json: Json,
) : RecipesApiClient {

    private val baseUrl: String = config.normalizedBaseUrl
    private val recipesUrl: String = baseUrl + RecipesApi.RecipesPath
    private val tagsUrl: String = baseUrl + RecipesApi.TagsPath

    override suspend fun getRecipes(query: RecipesQuery): Page<Recipe> =
        getPage(recipesUrl, query.toQueryParameters(), "recipes")

    override suspend fun getRecipe(id: Int): Recipe =
        getJson("$recipesUrl/$id", "recipe $id") { RecipeDto.serializer() }.toDomain()

    override suspend fun searchRecipes(query: String, page: RecipesQuery): Page<Recipe> {
        if (query.isBlank()) {
            throw RecipesInvalidRequestFailure("The search query must not be blank.")
        }
        // The raw query text is handed to Ktor, which encodes query parameters.
        val parameters = page.toQueryParameters() + ("q" to query)
        return getPage("$recipesUrl/${RecipesApi.SearchPathSegment}", parameters, "search")
    }

    override suspend fun getRecipesByTag(tag: String, page: RecipesQuery): Page<Recipe> {
        if (tag.isBlank()) {
            throw RecipesInvalidRequestFailure("The tag must not be blank.")
        }
        return getPage(
            url = "$recipesUrl/${RecipesApi.TagPathSegment}/${tag.encodeURLParameter()}",
            parameters = page.toQueryParameters(),
            label = "tag '$tag'",
        )
    }

    override suspend fun getRecipesByMealType(mealType: String, page: RecipesQuery): Page<Recipe> {
        if (mealType.isBlank()) {
            throw RecipesInvalidRequestFailure("The meal type must not be blank.")
        }
        return getPage(
            url = "$recipesUrl/${RecipesApi.MealTypePathSegment}/${mealType.encodeURLParameter()}",
            parameters = page.toQueryParameters(),
            label = "meal type '$mealType'",
        )
    }

    override suspend fun getTags(): List<String> = request(
        method = HttpMethod.Get,
        url = tagsUrl,
        label = "tags",
    ) { response -> decode(response) { json.decodeFromString<List<String>>(response) } }

    override suspend fun addRecipe(recipe: NewRecipe): Recipe {
        if (recipe.name.isBlank()) {
            throw RecipesInvalidRequestFailure("A new recipe needs a non-blank name.")
        }
        val payload = json.encodeToString(NewRecipeDto.serializer(), NewRecipeDto.from(recipe))
        return request(
            method = HttpMethod.Post,
            url = "$recipesUrl/${RecipesApi.AddPathSegment}",
            label = "add recipe",
            body = payload,
        ) { response -> decode(response) { json.decodeFromString(RecipeDto.serializer(), response).toDomain() } }
    }

    override suspend fun updateRecipe(id: Int, update: RecipeUpdate, patch: Boolean): Recipe {
        if (update.isEmpty) {
            throw RecipesInvalidRequestFailure("A recipe update must contain at least one changed field.")
        }
        val payload = json.encodeToString(RecipeUpdateDto.serializer(), RecipeUpdateDto.from(update))
        val verb = if (patch) UpdateVerb.Patch else UpdateVerb.Put
        return request(
            method = verb.httpMethod,
            url = "$recipesUrl/$id",
            label = "update recipe $id",
            body = payload,
        ) { response -> decode(response) { json.decodeFromString(RecipeDto.serializer(), response).toDomain() } }
    }

    override suspend fun deleteRecipe(id: Int): DeletedRecipe = request(
        method = HttpMethod.Delete,
        url = "$recipesUrl/$id",
        label = "delete recipe $id",
    ) { response ->
        decode(response) { json.decodeFromString(RecipeDto.serializer(), response).toDeletedRecipe() }
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private suspend fun getPage(url: String, parameters: Map<String, String>, label: String): Page<Recipe> =
        request(method = HttpMethod.Get, url = url, label = label, parameters = parameters) { response ->
            decode(response) { json.decodeFromString(RecipesPageDto.serializer(), response) }
        }.let(RecipesMapper::toPage)

    private suspend fun <T> getJson(
        url: String,
        label: String,
        serializer: () -> kotlinx.serialization.KSerializer<T>,
    ): T = request(method = HttpMethod.Get, url = url, label = label) { response ->
        decode(response) { json.decodeFromString(serializer(), response) }
    }

    private suspend fun <T> request(
        method: HttpMethod,
        url: String,
        label: String,
        parameters: Map<String, String> = emptyMap(),
        body: String? = null,
        decode: (String) -> T,
    ): T {
        val response: HttpResponse = httpClient.request(url) {
            this.method = method
            parameters.forEach { (key, value) -> parameter(key, value) }
            if (body != null) {
                contentType(ContentType.Application.Json)
                header("Accept", ContentType.Application.Json.toString())
                setBody(body)
            }
        }
        if (!response.status.isSuccess()) {
            throw httpFailure(response, response.bodyAsText())
        }
        return decode(response.bodyAsText())
    }

    private fun <T> decode(raw: String, parse: () -> T): T = try {
        parse()
    } catch (error: SerializationException) {
        throw RecipesSerializationFailure(
            message = "The recipes API returned a body that does not match the expected schema for this call: " +
                (error.message ?: "unknown reason"),
            cause = error,
        )
    } catch (error: IllegalArgumentException) {
        throw RecipesSerializationFailure(
            message = "The recipes API returned a body that could not be decoded: " +
                (error.message ?: "unknown reason"),
            cause = error,
        )
    }
}
