package com.example.myapplication.recipes.model

import kotlinx.serialization.Serializable

/**
 * A single recipe from the DummyJSON recipes dataset.
 *
 * Every collection property defaults to an empty list and every scalar to a
 * neutral value, because the API can return partial objects when the `select`
 * query parameter is used.
 */
@Serializable
public data class Recipe(
    /** Unique identifier of the recipe. */
    val id: Int = 0,
    /** Human readable recipe name, e.g. `"Classic Margherita Pizza"`. */
    val name: String = "",
    /** Ingredient lines, one per ingredient. */
    val ingredients: List<String> = emptyList(),
    /** Ordered preparation steps. */
    val instructions: List<String> = emptyList(),
    /** Preparation time in minutes. */
    val prepTimeMinutes: Int = 0,
    /** Cooking time in minutes. */
    val cookTimeMinutes: Int = 0,
    /** Number of servings the recipe yields. */
    val servings: Int = 0,
    /** Difficulty label, typically `"Easy"`, `"Medium"` or `"Hard"`. */
    val difficulty: String = "",
    /** Cuisine of origin, e.g. `"Italian"`. */
    val cuisine: String = "",
    /** Calories per serving. */
    val caloriesPerServing: Int = 0,
    /** Free-form tags such as `"Pizza"` or `"Vegetarian"`. */
    val tags: List<String> = emptyList(),
    /** Identifier of the user that contributed the recipe. */
    val userId: Int = 0,
    /** Absolute URL of the recipe image (webp). */
    val image: String = "",
    /** Average user rating. */
    val rating: Double = 0.0,
    /** Number of submitted reviews. */
    val reviewCount: Int = 0,
    /** Meal types the recipe belongs to, e.g. `["Dinner"]`. */
    val mealType: List<String> = emptyList(),
) {
    /** Total time in minutes needed to bring the recipe to the table. */
    public val totalTimeMinutes: Int
        get() = prepTimeMinutes + cookTimeMinutes

    /** `true` when at least one ingredient is present. */
    public val hasIngredients: Boolean
        get() = ingredients.isNotEmpty()

    /** `true` when at least one instruction step is present. */
    public val hasInstructions: Boolean
        get() = instructions.isNotEmpty()
}
