package io.kudos.base.model.payload

import io.kudos.base.query.sort.Order

/**
 * Payload for list-query condition items.
 *
 * @author K
 * @since 1.0.0
 */
open class ListSearchPayload : ISearchPayload {

    /** Current page number (null means no pagination). */
    open var pageNo: Int? = null

    /** Page size (only applied when pageNo is non-null). */
    open var pageSize: Int? = null

    /**
     * Upper bound on the per-page row count; used to prevent pageSize from being maliciously inflated.
     * The effective page size is min(pageSize, getMaxPageSize()).
     * Subclasses may override to adjust the limit.
     */
    open fun getMaxPageSize(): Int = 100

    /**
     * Whether unpaged queries (all rows) are allowed when [pageNo] is null.
     * When true, a null pageNo applies no limit; when false, a null pageNo falls back to page 1 to avoid
     * full-table scans. Defaults to false. Subclasses should override to true only in deliberate trusted
     * scenarios (e.g. exports, background jobs) or for small tables.
     */
    open fun isUnpagedSearchAllowed(): Boolean = false

    /**
     * Sort request (property name and direction).
     * Every property participating in sorting must be annotated with [io.kudos.base.query.sort.Sortable] on the
     * DAO's corresponding table entity (PO); unannotated entries are ignored with a WARN log, regardless of
     * whether [getReturnEntityClass] is a VO.
     */
    open var orders: List<Order>? = null

}
