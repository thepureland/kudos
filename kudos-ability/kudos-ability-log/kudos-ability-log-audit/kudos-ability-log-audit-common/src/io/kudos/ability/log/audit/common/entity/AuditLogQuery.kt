package io.kudos.ability.log.audit.common.entity

import java.io.Serializable
import java.time.LocalDateTime

/**
 * Query filter for [io.kudos.ability.log.audit.common.api.IAuditLogReadOnlyService.pagingSearch].
 *
 * All fields are nullable — `null` means "no filter on this dimension". Multiple non-null fields
 * combine with AND. Implementations are expected to reject queries whose filters all resolve to
 * null when running against a high-volume backend (no filter + paging across millions of rows is
 * an operator-pageable, not a developer-pageable, query); for first-port simplicity the contract
 * does not mandate that — callers are expected to pass a sensible time window.
 *
 * The filter surface is intentionally small. Soul's MyBatis variant ships a wider AuditLogQueryModel
 * that includes view-table joins (operator name, module hierarchy expanded, etc.); those derived
 * fields can be handled by the caller composing the result with sys-user / sys-module lookups, so
 * they don't belong in the storage-side query contract.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class AuditLogQuery : Serializable {

    /** Tenant id — exact match. Required for cross-tenant deployments to honor data isolation. */
    var tenantId: String? = null

    /** Source tenant id — exact match. Distinguishes the tenant the audit was attributed to from the tenant the user belonged to. */
    var sourceTenantId: String? = null

    /** Subsystem code (e.g. "user", "auth") — exact match. */
    var subSysCode: String? = null

    /** Module code — exact match. */
    var moduleCode: String? = null

    /** Operation type id from the dict — exact match. */
    var operateTypeId: Int? = null

    /** Operator user id — exact match. Most common dashboard filter ("who did this"). */
    var operatorId: String? = null

    /** Operator user type — exact match. */
    var operatorUserType: String? = null

    /** Business entity id (the affected object's id) — exact match. */
    var entityId: String? = null

    /** Inclusive lower bound on `operate_time`. `null` means no lower bound. */
    var operateTimeFrom: LocalDateTime? = null

    /** Exclusive upper bound on `operate_time`. `null` means no upper bound. */
    var operateTimeTo: LocalDateTime? = null

    /**
     * Fuzzy match on `description` (`LIKE '%value%'`). `null` skips the filter. Empty string is
     * treated the same as `null` so an empty UI input doesn't accidentally generate a `LIKE '%%'`.
     */
    var descriptionLike: String? = null

    companion object {
        private const val serialVersionUID = 1L
    }
}
