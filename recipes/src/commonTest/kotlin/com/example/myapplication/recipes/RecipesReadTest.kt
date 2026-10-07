package com.example.myapplication.recipes

import com.example.myapplication.recipes.TestFixtures.bodyOf
import com.example.myapplication.recipes.TestFixtures.engine
import com.example.myapplication.recipes.TestFixtures.failure
import com.example.myapplication.recipes.TestFixtures.jsonEngine
import com.example.myapplication.recipes.TestFixtures.repository
import com.example.myapplication.recipes.TestFixtures.value
import com.example.myapplication.recipes.model.NewRecipe
import com.example.myapplication.recipes.model.RecipesQuery
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Read-path tests for [RecipesRepository]. */
internal class RecipesReadTest : RecipesTestBase() {

    @Test
    fun getRecipesMapsPayloadAndPagination() = recipeTest {
        val engine = jsonEngine(body = TestFixtures.PageJson)
        val repository = repository(engine)

        val page = repository.getRecipes(RecipesQuery(limit = 2, skip = 10)).value()

        assertEquals(2, page.items.size)
        assertEquals("Classic Margherita Pizza", page.items[0].name)
        assertEquals(35, page.items[0].totalTimeMinutes)
        assertEquals(50, page.total)
        assertEquals(10, page.skip)
        assertEquals(2, page.limit)
        assertTrue(page.hasMore)
        assertEquals(12, page.nextSkip())
        assertEquals(5, page.pageIndex)
        assertEquals(25, page.pageCount)

        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("/recipes", request.url.encodedPath)
        assertEquals("2", request.url.parameters["limit"])
        assertEquals("10", request.url.parameters["skip"])
    }

    @Test
    fun getRecipesKeepsPartialPayloadsUsable() = recipeTest {
        val repository = repository(jsonEngine(body = TestFixtures.PageJson))

        val page = repository.getRecipes().value()

        // The second recipe only carries id, name and image.
        val partial = page.items[1]
        assertEquals(2, partial.id)
        assertEquals("Chicken Biryani", partial.name)
        assertTrue(partial.ingredients.isEmpty())
        assertTrue(partial.tags.isEmpty())
        assertEquals(0.0, partial.rating)
    }

    @Test
    fun getRecipesOmitsSkipWhenZero() = recipeTest {
        val engine = jsonEngine(body = TestFixtures.PageJson)

        repository(engine).getRecipes(RecipesQuery(limit = 30))

        val request = engine.requestHistory.single()
        assertEquals("30", request.url.parameters["limit"])
        assertNull(request.url.parameters["skip"])
    }

    @Test
    fun getRecipesRejectsNegativeLimit() {
        var thrown: IllegalArgumentException? = null
        try {
            RecipesQuery(limit = -1)
        } catch (error: IllegalArgumentException) {
            thrown = error
        }
        assertTrue(thrown != null, "A negative limit must be rejected")
    }

    @Test
    fun getRecipeByIdReturnsSingleRecipe() = recipeTest {
        val engine = jsonEngine()
        val repository = repository(engine)

        val recipe = repository.getRecipe(1).value()

        assertEquals(1, recipe.id)
        assertEquals("Classic Margherita Pizza", recipe.name)
        assertEquals(4, recipe.servings)
        assertEquals("Italian", recipe.cuisine)
        assertEquals(4.6, recipe.rating)
        assertEquals(listOf("Dinner"), recipe.mealType)
        assertTrue(recipe.hasIngredients)
        assertTrue(recipe.hasInstructions)

        assertEquals("/recipes/1", engine.requestHistory.single().url.encodedPath)
    }

    @Test
    fun getRecipeRejectsNonPositiveIdWithoutCallingTheApi() = recipeTest {
        val engine = jsonEngine()
        val repository = repository(engine)

        val failure = repository.getRecipe(0).failure()

        assertEquals(RecipesFailureKind.InvalidRequest, failure.kind)
        assertTrue(engine.requestHistory.isEmpty(), "No HTTP call should be made for an invalid id")
    }

    @Test
    fun getRecipeReportsHttpErrorWithStatusAndBody() = recipeTest {
        val engine = engine { _ ->
            respond(
                content = TestFixtures.ErrorJson,
                status = HttpStatusCode.NotFound,
                headers = headersOf("Content-Type", "application/json"),
            )
        }
        val repository = repository(engine)

        val failure = repository.getRecipe(999).failure()

        assertIs<RecipesHttpFailure>(failure)
        assertEquals(404, failure.statusCode)
        assertFalse(failure.isServerError)
        assertFalse(failure.isRateLimited)
        assertEquals(RecipesFailureKind.Http, failure.kind)
        assertContains(failure.message.orEmpty(), "not found")
    }

    @Test
    fun searchRecipesSendsEncodedQuery() = recipeTest {
        val engine = jsonEngine(body = TestFixtures.PageJson)
        val repository = repository(engine)

        repository.searchRecipes("pizza & pasta").value()

        val request = engine.requestHistory.single()
        assertEquals("/recipes/search", request.url.encodedPath)
        assertEquals("pizza & pasta", request.url.parameters["q"])
        // The value must be encoded exactly once on the wire.
        assertEquals("limit=30&q=pizza+%26+pasta", request.url.encodedQuery)
    }

    @Test
    fun getRecipesByTagEncodesPathSegmentOnce() = recipeTest {
        val engine = jsonEngine(body = TestFixtures.PageJson)

        repository(engine).getRecipesByTag("main course").value()

        val request = engine.requestHistory.single()
        assertEquals("main course", request.url.segments.last())
        assertEquals("main%20course", request.url.encodedPath.substringAfterLast('/'))
    }

    @Test
    fun searchRecipesRejectsBlankQuery() = recipeTest {
        val engine = jsonEngine()

        val failure = repository(engine).searchRecipes("   ").failure()

        assertEquals(RecipesFailureKind.InvalidRequest, failure.kind)
        assertTrue(engine.requestHistory.isEmpty())
    }

    @Test
    fun getRecipesByTagUsesTagPathVariable() = recipeTest {
        val engine = jsonEngine(body = TestFixtures.PageJson)

        repository(engine).getRecipesByTag("Pakistani").value()

        val request = engine.requestHistory.single()
        assertEquals("/recipes/tag/Pakistani", request.url.encodedPath)
    }

    @Test
    fun getRecipesByMealTypeUsesMealTypePathVariable() = recipeTest {
        val engine = jsonEngine(body = TestFixtures.PageJson)

        repository(engine).getRecipesByMealType("snack").value()

        assertEquals("/recipes/meal-type/snack", engine.requestHistory.single().url.encodedPath)
    }

    @Test
    fun getTagsDecodesStringArray() = recipeTest {
        val engine = jsonEngine(body = TestFixtures.TagsJson)

        val tags = repository(engine).getTags().value()

        assertEquals(listOf("Pizza", "Italian", "Vegetarian"), tags)
        assertEquals("/recipes/tags", engine.requestHistory.single().url.encodedPath)
    }

    @Test
    fun sortingIsSentAsQueryParameters() = recipeTest {
        val engine = jsonEngine(body = TestFixtures.PageJson)

        repository(engine).getRecipes(
            RecipesQuery.build {
                limit = 10
                sortByDescending(RecipeSortField.Rating)
                setSelect(listOf(RecipeSelectField.Name, RecipeSelectField.Image))
            },
        ).value()

        val parameters = engine.requestHistory.single().url.parameters
        assertEquals("rating", parameters["sortBy"])
        assertEquals(RecipeSortOrder.Descending, parameters["order"])
        assertEquals("name,image", parameters["select"])
    }

    @Test
    fun getRecipesByDifficultyFiltersThePage() = recipeTest {
        val repository = repository(jsonEngine(body = TestFixtures.PageJson))

        val page = repository.getRecipesByDifficulty("easy").value()

        assertEquals(1, page.size)
        assertEquals("Classic Margherita Pizza", page.items.single().name)
        assertEquals(1, page.total)
    }

    @Test
    fun getRecipesByCuisineFiltersThePage() = recipeTest {
        val repository = repository(jsonEngine(body = TestFixtures.PageJson))

        val page = repository.getRecipesByCuisine("ITALIAN").value()

        assertEquals(1, page.size)
        assertEquals("Classic Margherita Pizza", page.items.single().name)
    }

    @Test
    fun getAllRecipesFollowsPagesUntilExhausted() = recipeTest {
        var call = 0
        val engine = engine { _ ->
            call += 1
            val body = if (call == 1) {
                """{"recipes":[{"id":1,"name":"A"},{"id":2,"name":"B"}],"total":3,"skip":0,"limit":2}"""
            } else {
                """{"recipes":[{"id":3,"name":"C"}],"total":3,"skip":2,"limit":2}"""
            }
            respond(content = body, status = HttpStatusCode.OK, headers = headersOf("Content-Type", "application/json"))
        }

        val recipes = repository(engine).getAllRecipes(pageSize = 2).value()

        assertEquals(listOf(1, 2, 3), recipes.map { it.id })
        assertEquals(2, engine.requestHistory.size)
        assertEquals("0", engine.requestHistory[0].url.parameters["skip"] ?: "0")
        assertEquals("2", engine.requestHistory[1].url.parameters["skip"])
    }

    @Test
    fun requestBodiesAndHeadersRemainJsonForWrites() = recipeTest {
        val engine = jsonEngine()
        val repository = repository(engine)

        repository.addRecipe(NewRecipe(name = "Tasty Pizza")).value()

        val request = engine.requestHistory.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/recipes/add", request.url.encodedPath)
        assertEquals("application/json", request.body.contentType?.withoutParameters().toString())
        assertTrue(bodyOf(request).contains("\"name\":\"Tasty Pizza\""))
    }
}
