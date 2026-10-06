package com.example.myapplication.recipes

/**
 * API surface of the DummyJSON recipes endpoint.
 *
 * These values are the literal path segments/query keys accepted by the API,
 * so they are safe to use when building URLs by hand.
 */
public object RecipesApi {
    /** Scheme + host + base path of the DummyJSON API. */
    public const val BaseUrl: String = "https://dummyjson.com"

    /** Path of the recipes collection. */
    public const val RecipesPath: String = "/recipes"

    /** Path of the list of all known recipe tags. */
    public const val TagsPath: String = "/recipes/tags"

    /** Path segment used by the "recipes by tag" endpoint. */
    public const val TagPathSegment: String = "tag"

    /** Path segment used by the "recipes by meal type" endpoint. */
    public const val MealTypePathSegment: String = "meal-type"

    /** Path segment appended for search requests. */
    public const val SearchPathSegment: String = "search"

    /** Path segment appended for add requests. */
    public const val AddPathSegment: String = "add"

    /** Default number of items returned by the API when no limit is given. */
    public const val DefaultLimit: Int = 30

    /** Pass `limit = 0` to the API to receive every item. */
    public const val AllItemsLimit: Int = 0
}
