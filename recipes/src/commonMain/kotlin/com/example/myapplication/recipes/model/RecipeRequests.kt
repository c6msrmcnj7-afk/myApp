package com.example.myapplication.recipes.model

/**
 * Payload accepted by `POST /recipes/add`.
 *
 * DummyJSON does not persist anything: the call is simulated and the response
 * is the created recipe with a fresh id. Every non-null field is serialized, so
 * leaving a field `null` keeps it out of the request body.
 */
public data class NewRecipe(
    /** Name of the recipe. Required and must not be blank. */
    val name: String,
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
)

/**
 * Partial update accepted by `PUT`/`PATCH /recipes/{id}`.
 *
 * Only the non-null fields are sent. Use [merge] to combine updates that were
 * assembled independently.
 */
public data class RecipeUpdate(
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
    /** `true` when nothing would be sent to the server. */
    public val isEmpty: Boolean
        get() = name == null &&
            ingredients == null &&
            instructions == null &&
            prepTimeMinutes == null &&
            cookTimeMinutes == null &&
            servings == null &&
            difficulty == null &&
            cuisine == null &&
            caloriesPerServing == null &&
            tags == null &&
            userId == null &&
            image == null &&
            rating == null &&
            reviewCount == null &&
            mealType == null

    /**
     * Returns an update carrying every non-null field of [other] on top of this
     * one; fields that are `null` in [other] keep their current value.
     */
    public fun merge(other: RecipeUpdate): RecipeUpdate = RecipeUpdate(
        name = other.name ?: name,
        ingredients = other.ingredients ?: ingredients,
        instructions = other.instructions ?: instructions,
        prepTimeMinutes = other.prepTimeMinutes ?: prepTimeMinutes,
        cookTimeMinutes = other.cookTimeMinutes ?: cookTimeMinutes,
        servings = other.servings ?: servings,
        difficulty = other.difficulty ?: difficulty,
        cuisine = other.cuisine ?: cuisine,
        caloriesPerServing = other.caloriesPerServing ?: caloriesPerServing,
        tags = other.tags ?: tags,
        userId = other.userId ?: userId,
        image = other.image ?: image,
        rating = other.rating ?: rating,
        reviewCount = other.reviewCount ?: reviewCount,
        mealType = other.mealType ?: mealType,
    )
}

/**
 * Result of `DELETE /recipes/{id}`.
 *
 * @param id identifier of the recipe the API pretended to delete.
 * @param isDeleted always `true` for a successful DummyJSON response.
 * @param deletedOn ISO-8601 timestamp reported by the API, when present.
 * @param recipe the deleted recipe as echoed back by the API.
 */
public data class DeletedRecipe(
    val id: Int,
    val isDeleted: Boolean,
    val deletedOn: String? = null,
    val recipe: Recipe? = null,
)
