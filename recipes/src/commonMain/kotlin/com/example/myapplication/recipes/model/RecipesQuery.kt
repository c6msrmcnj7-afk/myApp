package com.example.myapplication.recipes.model

import com.example.myapplication.recipes.RecipeSortOrder
import com.example.myapplication.recipes.RecipesApi

/**
 * Immutable description of a `GET /recipes` request.
 *
 * ```kotlin
 * val query = RecipesQuery.build {
 *     limit = 10
 *     skip = 20
 *     sortByDescending(RecipeSortField.Rating)
 *     setSelect(listOf(RecipeSelectField.Name, RecipeSelectField.Image))
 * }
 * ```
 */
public data class RecipesQuery(
    /** Maximum number of recipes to return. `0` asks the API for all of them. */
    val limit: Int = RecipesApi.DefaultLimit,
    /** Number of recipes to skip, used for pagination. */
    val skip: Int = 0,
    /** Field name to sort by, see [com.example.myapplication.recipes.RecipeSortField]. */
    val sortBy: String? = null,
    /** Sort direction, see [com.example.myapplication.recipes.RecipeSortOrder]. */
    val order: String? = null,
    /** Field names to project, see [com.example.myapplication.recipes.RecipeSelectField]. */
    val select: List<String> = emptyList(),
) {
    init {
        require(limit >= 0) { "limit must be >= 0 but was $limit" }
        require(skip >= 0) { "skip must be >= 0 but was $skip" }
    }

    /** Query parameters in the order the API expects them; `null`/empty values are dropped. */
    public fun toQueryParameters(): Map<String, String> = buildMap {
        put("limit", limit.toString())
        if (skip != 0) put("skip", skip.toString())
        sortBy?.takeIf { it.isNotBlank() }?.let { put("sortBy", it) }
        order?.takeIf { it.isNotBlank() }?.let { put("order", it) }
        if (select.isNotEmpty()) put("select", select.joinToString(","))
    }

    /** A copy of this query describing the page that follows the given one. */
    public fun nextPage(current: Page<*>): RecipesQuery =
        current.nextSkip()?.let { copy(skip = it, limit = if (limit == 0) current.limit else limit) } ?: this

    public companion object {
        /** DSL entry point, see the class documentation for an example. */
        public fun build(block: Builder.() -> Unit): RecipesQuery = Builder().apply(block).toQuery()

        /** Default query: the first page of recipes as returned by the API. */
        public fun firstPage(limit: Int = RecipesApi.DefaultLimit): RecipesQuery = RecipesQuery(limit = limit)

        /** Query that returns every recipe available on the server. */
        public fun all(): RecipesQuery = RecipesQuery(limit = RecipesApi.AllItemsLimit)
    }

    /** Mutable builder backing [RecipesQuery.build]. */
    public class Builder internal constructor() {
        /** Maximum number of recipes to return. `0` asks the API for all of them. */
        public var limit: Int = RecipesApi.DefaultLimit

        /** Number of recipes to skip, used for pagination. */
        public var skip: Int = 0

        private var sortByField: String? = null
        private var orderField: String? = null
        private var selectFields: List<String> = emptyList()

        /** Field to sort by, see [com.example.myapplication.recipes.RecipeSortField]. */
        public fun getSortBy(): String? = sortByField

        /** Sets the field to sort by. */
        public fun setSortBy(value: String?) {
            sortByField = value
        }

        /** Sort direction, see [com.example.myapplication.recipes.RecipeSortOrder]. */
        public fun getOrder(): String? = orderField

        /** Sets the sort direction. */
        public fun setOrder(value: String?) {
            orderField = value
        }

        /** Field names to project, see [com.example.myapplication.recipes.RecipeSelectField]. */
        public fun getSelect(): List<String> = selectFields

        /** Sets the field names to project. */
        public fun setSelect(value: List<String>) {
            selectFields = value
        }

        /** Sorts the result set in ascending order by [field]. */
        public fun sortByAscending(field: String) {
            sortByField = field
            orderField = RecipeSortOrder.Ascending
        }

        /** Sorts the result set in descending order by [field]. */
        public fun sortByDescending(field: String) {
            sortByField = field
            orderField = RecipeSortOrder.Descending
        }

        internal fun toQuery(): RecipesQuery = RecipesQuery(
            limit = limit,
            skip = skip,
            sortBy = sortByField,
            order = orderField,
            select = selectFields,
        )
    }
}
