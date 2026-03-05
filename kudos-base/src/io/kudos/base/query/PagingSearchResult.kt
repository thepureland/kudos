package io.kudos.base.query

/**
 * 分页查询结果
 *
 * @author K
 * @since 1.0.0
 */
data class PagingSearchResult<T>(
    /** 结果行对象列表 */
    val data : List<T>,
    /** 总行数 */
    val totalCount: Int,
)