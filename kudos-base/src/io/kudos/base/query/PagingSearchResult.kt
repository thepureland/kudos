package io.kudos.base.query

/**
 * Paged search result.
 *
 * @author K
 * @since 1.0.0
 */
data class PagingSearchResult<T>(
    /** List of result row objects. */
    val data : List<T>,
    /** Total row count. */
    val totalCount: Int,
)