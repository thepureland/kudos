package io.kudos.base.support.payload

import io.kudos.base.query.sort.Order

/**
 * 列表查询条件项载体
 *
 * @author K
 * @since 1.0.0
 */
open class ListSearchPayload : ISearchPayload {

    /** 当前页码(为null不分页) */
    open var pageNo: Int? = null

    /** 页面大小(仅当pageNo不为null时才应用) */
    open var pageSize: Int? = null

    /**
     * 单页最大条数上限，用于防止 pageSize 被恶意放大。
     * 实际分页时取 min(pageSize, getMaxPageSize())。
     * 子类可重写以调整上限。
     */
    open fun getMaxPageSize(): Int = 100

    /**
     * 可排序字段白名单（属性名集合）。
     * 仅当 [orders] 中的属性名在此集合内时才会参与排序，避免恶意排序字段。
     * 默认为空表示不接受来自 payload 的排序；子类可重写并返回允许的字段名集合。
     */
    open fun getSortableProperties(): Set<String> = emptySet()

    /** 排序规则 */
    open var orders: List<Order>? = null

}