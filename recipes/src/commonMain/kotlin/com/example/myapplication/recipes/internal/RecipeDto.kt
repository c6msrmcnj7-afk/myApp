package com.example.myapplication.recipes.internal

import com.example.myapplication.recipes.model.DeletedRecipe
import com.example.myapplication.recipes.model.NewRecipe
import com.example.myapplication.recipes.model.Recipe
import com.example.myapplication.recipes.model.RecipeUpdate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire representation of a recipe.
 *
 * Kept separate from the public [Recipe] model so that a change on the server
 * never breaks the published API of this library.
 */
@Serializable
internal data class RecipeDto(
    val id: Int = 0,
    val name: String = "",
    val ingredients: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val prepTimeMinutes: Int = 0,
    val cookTimeMinutes: Int = 0,
    val servings: Int = 0,
    val difficulty: String = "",
    val cuisine: String = "",
    val caloriesPerServing: Int = 0,
    val tags: List<String> = emptyList(),
    val userId: Int = 0,
    val image: String = "",
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val mealType: List<String> = emptyList(),
    /** Present only on delete responses. */
    val isDeleted: Boolean? = null,
    /** Present only on delete responses. */
    val deletedOn: String? = null,
) {
    fun toDomain(): Recipe = Recipe(
        id = id,
        name = name,
        ingredients = ingredients,
        instructions = instructions,
        prepTimeMinutes = prepTimeMinutes,
        cookTimeMinutes = cookTimeMinutes,
        servings = servings,
        difficulty = difficulty,
        cuisine = cuisine,
        caloriesPerServing = caloriesPerServing,
        tags = tags,
        userId = userId,
        image = image,
        rating = rating,
        reviewCount = reviewCount,
        mealType = mealType,
    )

    fun toDeletedRecipe(): DeletedRecipe = DeletedRecipe(
        id = id,
        isDeleted = isDeleted ?: true,
        deletedOn = deletedOn,
        recipe = toDomain(),
    )
}

/** `GET /recipes`, `GET /recipes/search`, `GET /recipes/tag/{tag}`, `GET /recipes/meal-type/{meal}`. */
@Serializable
internal data class RecipesPageDto(
    val recipes: List<RecipeDto> = emptyList(),
    val total: Int = 0,
    val skip: Int = 0,
    val limit: Int = 0,
)

/**
 * Request body for `POST /recipes/add`.
 *
 * Null fields are omitted so the simulated server only receives what the
 * caller supplied.
 */
@Serializable
internal data class NewRecipeDto(
    @SerialName("name") val name: String,
    val ingredients: List<String>? = null,
    val instructions: List<String>? = null,
    val prepTimeMinutes: Int? = null,
    val cookTimeMinutes: Int? = null,
    val servings: Int? = null,
    val difficulty: String? = null,
    val cuisine: String? = null,
    val caloriesPerServing: Int? = null,
    val tags: List<String>? = null,
    val userId: Int? = null,
    val image: String? = null,
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val mealType: List<String>? = null,
) {
    companion object {
        fun from(request: NewRecipe): NewRecipeDto = NewRecipeDto(
            name = request.name,
            ingredients = request.ingredients,
            instructions = request.instructions,
            prepTimeMinutes = request.prepTimeMinutes,
            cookTimeMinutes = request.cookTimeMinutes,
            servings = request.servings,
            difficulty = request.difficulty,
            cuisine = request.cuisine,
            caloriesPerServing = request.caloriesPerServing,
            tags = request.tags,
            userId = request.userId,
            image = request.image,
            rating = request.rating,
            reviewCount = request.reviewCount,
            mealType = request.mealType,
        )
    }
}

/** Request body for `PUT`/`PATCH /recipes/{id}`. */
@Serializable
internal data class RecipeUpdateDto(
    val name: String? = null,
    val ingredients: List<String>? = null,
    val instructions: List<String>? = null,
    val prepTimeMinutes: Int? = null,
    val cookTimeMinutes: Int? = null,
    val servings: Int? = null,
    val difficulty: String? = null,
    val cuisine: String? = null,
    val caloriesPerServing: Int? = null,
    val tags: List<String>? = null,
    val userId: Int? = null,
    val image: String? = null,
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val mealType: List<String>? = null,
) {
    companion object {
        fun from(update: RecipeUpdate): RecipeUpdateDto = RecipeUpdateDto(
            name = update.name,
            ingredients = update.ingredients,
            instructions = update.instructions,
            prepTimeMinutes = update.prepTimeMinutes,
            cookTimeMinutes = update.cookTimeMinutes,
            servings = update.servings,
            difficulty = update.difficulty,
            cuisine = update.cuisine,
            caloriesPerServing = update.caloriesPerServing,
            tags = update.tags,
            userId = update.userId,
            image = update.image,
            rating = update.rating,
            reviewCount = update.reviewCount,
            mealType = update.mealType,
        )
    }
}

/** Error body returned by DummyJSON for 4xx/5xx responses. */
@Serializable
internal data class ApiErrorDto(
    val message: String? = null,
)
