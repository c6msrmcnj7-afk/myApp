package com.example.myapplication.recipes.model

/**
 * One page of results returned by a paginated DummyJSON endpoint.
 *
 * @param items the entities contained in this page.
 * @param total total number of entities available on the server.
 * @param skip number of entities skipped before this page.
 * @param limit maximum number of entities this page could contain.
 */
public data class Page<T>(
    val items: List<T>,
    val total: Int,
    val skip: Int,
    val limit: Int,
) {
    /** Number of entities in this page. */
    public val size: Int
        get() = items.size

    /** `true` when the server holds more entities after this page. */
    public val hasMore: Boolean
        get() = skip + items.size < total

    /** `true` when this page contains no entities. */
    public val isEmpty: Boolean
        get() = items.isEmpty()

    /**
     * The `skip` value to send for the next page, or `null` when this is the
     * last page.
     */
    public fun nextSkip(): Int? = if (hasMore) skip + items.size else null

    /** Zero-based index of this page, derived from [skip] and [limit]. */
    public val pageIndex: Int
        get() = if (limit > 0) skip / limit else 0

    /** Number of pages needed to consume [total] entities at this [limit]. */
    public val pageCount: Int
        get() = when {
            limit <= 0 -> if (total > 0) 1 else 0
            else -> (total + limit - 1) / limit
        }

    /** Transforms every item of this page while keeping the pagination data. */
    public fun <R> map(transform: (T) -> R): Page<R> =
        Page(items = items.map(transform), total = total, skip = skip, limit = limit)
}

/** An empty page, useful as a starting value. */
public fun <T> emptyPage(limit: Int = 0): Page<T> = Page(
    items = emptyList(),
    total = 0,
    skip = 0,
    limit = limit,
)
