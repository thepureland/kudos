package io.kudos.base.support.query

import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order

/**
 * 统一只读查询参数对象
 *
 * 用于收敛 `search` / `pagingSearch` / `count` 等方法的公共参数，
 * 以保持旧接口兼容的同时提供更稳定的扩展入口。
 *
 * @param criteria 查询条件，为null表示无条件
 * @param orders 排序规则，按顺序生效
 * @param pageNo 当前页码(从1开始)，与pageSize成对使用
 * @param pageSize 每页条数，与pageNo成对使用
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class ReadQuery(
    val criteria: Criteria? = null,
    val orders: List<Order> = emptyList(),
    val pageNo: Int? = null,
    val pageSize: Int? = null,
) {
    /** 是否启用分页（pageNo/pageSize 任一非空即视为启用） */
    val isPagingEnabled: Boolean
        get() = pageNo != null || pageSize != null

    /**
     * 获取分页参数并做合法性校验。
     *
     * @return Pair(pageNo, pageSize)
     * @throws IllegalArgumentException 当分页参数不完整或非法时抛出
     */
    fun requirePaging(): Pair<Int, Int> {
        val currentPageNo = requireNotNull(pageNo) { "ReadQuery.pageNo 不能为空（启用分页时必须指定）" }
        val currentPageSize = requireNotNull(pageSize) { "ReadQuery.pageSize 不能为空（启用分页时必须指定）" }
        require(currentPageNo > 0) { "ReadQuery.pageNo 必须大于0" }
        require(currentPageSize > 0) { "ReadQuery.pageSize 必须大于0" }
        return currentPageNo to currentPageSize
    }
}
