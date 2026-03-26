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
     * 排序请求（属性名与方向）。
     * 每个参与排序的属性均须在 DAO 对应表实体（PO）上标注 [io.kudos.base.query.sort.Sortable]；未标注的项会被忽略并记 WARN，与 [getReturnEntityClass] 是否为 VO 无关。
     */
    open var orders: List<Order>? = null

}