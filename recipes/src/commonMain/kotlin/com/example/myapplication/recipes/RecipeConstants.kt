package com.example.myapplication.recipes

/**
 * Sort direction accepted by the DummyJSON `order` query parameter.
 */
public object RecipeSortOrder {
    public const val Ascending: String = "asc"
    public const val Descending: String = "desc"
}

/**
 * Recipe fields that the API allows sorting by (`sortBy` query parameter).
 */
public object RecipeSortField {
    public const val Id: String = "id"
    public const val Name: String = "name"
    public const val PrepTimeMinutes: String = "prepTimeMinutes"
    public const val CookTimeMinutes: String = "cookTimeMinutes"
    public const val Servings: String = "servings"
    public const val Difficulty: String = "difficulty"
    public const val Cuisine: String = "cuisine"
    public const val CaloriesPerServing: String = "caloriesPerServing"
    public const val Rating: String = "rating"
    public const val ReviewCount: String = "reviewCount"
}

/**
 * Recipe fields that can be requested with the `select` query parameter.
 *
 * `select` accepts any recipe field name; the constants below cover the ones
 * consumers ask for most often.
 */
public object RecipeSelectField {
    public const val Id: String = "id"
    public const val Name: String = "name"
    public const val Ingredients: String = "ingredients"
    public const val Instructions: String = "instructions"
    public const val PrepTimeMinutes: String = "prepTimeMinutes"
    public const val CookTimeMinutes: String = "cookTimeMinutes"
    public const val Servings: String = "servings"
    public const val Difficulty: String = "difficulty"
    public const val Cuisine: String = "cuisine"
    public const val CaloriesPerServing: String = "caloriesPerServing"
    public const val Tags: String = "tags"
    public const val UserId: String = "userId"
    public const val Image: String = "image"
    public const val Rating: String = "rating"
    public const val ReviewCount: String = "reviewCount"
    public const val MealType: String = "mealType"
}
