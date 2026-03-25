package io.kudos.base.model.payload

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
     * 是否允许在 [pageNo] 为 null 时查询全部数据（不分页）。
     * 为 true 时，pageNo 为 null 则不施加 limit；为 false 时，pageNo 为 null 将按第 1 页分页，避免全表扫描。
     * 默认为 false。仅在有意的受信任场景（如导出、后台任务）或小数据量的表下子类重写为 true。
     */
    open fun isUnpagedSearchAllowed(): Boolean = false

    /**
     * 可排序字段白名单（属性名集合）。
     * 仅当 [orders] 中的属性名在此集合内时才会参与排序，避免恶意排序字段。
     * 默认为空表示不接受来自 payload 的排序；子类可重写并返回允许的字段名集合。
     */
    open fun getSortableProperties(): Set<String> = emptySet()

    /** 排序规则 */
    open var orders: List<Order>? = null

    /**
     * 值为 null 时仍作为查询条件的属性名（例如列 IS NULL）。
     * 见 [ISearchPayload.getNullProperties]。
     */
    open var nullProperties: List<String>? = null

    override fun getNullProperties(): List<String>? = nullProperties

}