package io.kudos.ability.log.audit.common.entity

import java.io.Serializable

/**
 * Paged result of [io.kudos.ability.log.audit.common.api.IAuditLogReadOnlyService.pagingSearch].
 *
 * Carries the slice of rows for the requested page plus the matching record total — implementations
 * must run **one** `count(*)` per call so the caller renders pagination controls without an extra
 * round-trip. [items] follows the table's chosen sort order (typically `operate_time DESC`).
 *
 * @param items rows for this page; ordered by the implementation's chosen default (descending operate-time)
 * @param total total matching records across all pages
 * @param pageNo 1-based page number that produced this slice (echoed back so callers don't have to remember)
 * @param pageSize requested page size (echoed back; the actual slice can be shorter on the last page)
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class AuditLogPage(
    val items: List<SysAuditLogVo>,
    val total: Long,
    val pageNo: Int,
    val pageSize: Int,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L

        /** Empty page convenience — when a query has no matches, callers can return this without allocating. */
        @JvmStatic
        fun empty(pageNo: Int, pageSize: Int): AuditLogPage =
            AuditLogPage(items = emptyList(), total = 0L, pageNo = pageNo, pageSize = pageSize)
    }
}
