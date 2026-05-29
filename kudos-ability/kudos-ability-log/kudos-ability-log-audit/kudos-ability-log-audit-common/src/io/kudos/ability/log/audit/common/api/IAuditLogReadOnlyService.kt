package io.kudos.ability.log.audit.common.api

import io.kudos.ability.log.audit.common.entity.AuditLogPage
import io.kudos.ability.log.audit.common.entity.AuditLogQuery
import io.kudos.ability.log.audit.common.entity.SysAuditDetailLogVo
import io.kudos.ability.log.audit.common.entity.SysAuditLogVo

/**
 * Read-side companion to [IAuditService]. The write path ([IAuditService.submit]) ingests
 * `SysAuditLogModel`s produced by the audit aspect; this contract serves the **admin / forensics
 * use case** of looking the same records back up.
 *
 * Backed by whatever storage the deployment chose:
 *  - `kudos-ability-log-audit-rdb-ktorm` returns rows from `sys_audit_log` / `sys_audit_detail_log`.
 *  - Future backends (ClickHouse / Mongo) implement the same interface so the admin UI doesn't have
 *    to switch query code based on storage engine.
 *
 * The interface is intentionally **narrow**: single lookups by id, plus a paged-and-filtered search.
 * Derived presentation concerns (operator name resolution, module hierarchy expansion, etc.) belong
 * outside this contract — they require joins into sys-user / sys-module which are domain concerns
 * the storage layer should not encode.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IAuditLogReadOnlyService {

    /**
     * Looks up a single audit-log main-table row by id. Returns `null` when no row matches —
     * implementations must **not** throw for missing ids; absent rows are a valid signal for admin
     * UIs (audit ids can be forwarded around and re-queried after retention purges).
     */
    fun findById(id: String): SysAuditLogVo?

    /**
     * Looks up the 1:1 detail row for an audit-log main entry. The detail row carries the
     * request URL, raw request body (after `@LogDesensitize` masking), referer, and the
     * business-side description. Returns `null` when no detail row exists — older audit data
     * may have been written without a detail.
     */
    fun findDetailById(auditId: String): SysAuditDetailLogVo?

    /**
     * Paginated search across audit-log main-table rows.
     *
     * Filters are combined with AND; null fields on [query] are dropped. Ordering is
     * **implementation-chosen** — the Ktorm impl returns by `operate_time DESC` which matches the
     * typical admin "recent activity first" view. The returned [AuditLogPage.total] is a one-shot
     * count for paging controls; large unbounded queries (no tenant + no time window) are accepted
     * but discouraged at the deployment level.
     *
     * @param pageNo 1-based; values < 1 are clamped to 1
     * @param pageSize values < 1 are clamped to 1; implementations may further cap this
     */
    fun pagingSearch(query: AuditLogQuery, pageNo: Int, pageSize: Int): AuditLogPage
}
