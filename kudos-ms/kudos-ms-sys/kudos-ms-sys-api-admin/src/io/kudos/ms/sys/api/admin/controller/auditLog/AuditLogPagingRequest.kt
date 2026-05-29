package io.kudos.ms.sys.api.admin.controller.auditLog

import io.kudos.ability.log.audit.common.entity.AuditLogQuery
import java.io.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Wire-format request VO for [SysAuditLogAdminController.pagingSearch]. Combines the storage-side
 * [AuditLogQuery] filter dimensions with paging fields (`pageNo` / `pageSize`) that the storage
 * contract takes as separate args.
 *
 * Datetime fields are accepted as `yyyy-MM-dd HH:mm:ss` strings — matches the console UI's
 * `el-date-picker[datetimerange]` value-format. Spring + Jackson can deserialize LocalDateTime
 * directly when the format matches the ISO default, but the UI's format does not, so we take
 * strings here and parse in [toQuery] to avoid a global Jackson date-format config that would leak
 * to unrelated endpoints.
 *
 * Unknown / null fields are dropped on the way to [AuditLogQuery]; an "empty" request returns
 * everything (subject to the storage layer's order + cap).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class AuditLogPagingRequest(
    /** 1-based page index. Null defaults to 1. */
    var pageNo: Int? = null,
    /** Page size. Null defaults to 10 — consistent with other admin list endpoints. */
    var pageSize: Int? = null,

    /** Tenant id — exact match. */
    var tenantId: String? = null,
    /** Source tenant id — exact match. */
    var sourceTenantId: String? = null,
    /** Subsystem code — exact match. */
    var subSysCode: String? = null,
    /** Module code — exact match. */
    var moduleCode: String? = null,
    /** Module code fuzzy match. */
    var moduleCodeLike: String? = null,
    /** Operation type id (dict) — exact match. */
    var operateTypeId: Int? = null,
    /** Operation type display string — exact match. */
    var operateType: String? = null,
    /** Operator id — exact match. */
    var operatorId: String? = null,
    /** Operator display name fuzzy match — the dashboard's typical "who" filter. */
    var operatorLike: String? = null,
    /** Operator user type — exact match. */
    var operatorUserType: String? = null,
    /** Business entity id — exact match. */
    var entityId: String? = null,
    /** Inclusive lower bound on operate_time, "yyyy-MM-dd HH:mm:ss". */
    var operateTimeStart: String? = null,
    /** Exclusive upper bound on operate_time, "yyyy-MM-dd HH:mm:ss". */
    var operateTimeEnd: String? = null,
    /** Description fuzzy match. */
    var descriptionLike: String? = null,

    /**
     * Console list pages POST a generic search payload that may include `orders`; the storage layer
     * hard-codes operate_time DESC so we accept and discard the field instead of failing the
     * deserialization on an unknown property.
     */
    var orders: List<Any>? = null,
) : Serializable {

    fun toQuery(): AuditLogQuery {
        val q = AuditLogQuery()
        q.tenantId = tenantId
        q.sourceTenantId = sourceTenantId
        q.subSysCode = subSysCode
        // moduleCode (exact) takes precedence over moduleCodeLike when both are sent; in practice
        // the UI only sends one. We honour exact-match first because it's narrower.
        q.moduleCode = moduleCode
        q.moduleCodeLike = moduleCodeLike
        q.operateTypeId = operateTypeId
        q.operateType = operateType
        q.operatorId = operatorId
        q.operatorLike = operatorLike
        q.operatorUserType = operatorUserType
        q.entityId = entityId
        q.operateTimeFrom = parseLocalDateTime(operateTimeStart)
        q.operateTimeTo = parseLocalDateTime(operateTimeEnd)
        q.descriptionLike = descriptionLike
        return q
    }

    private fun parseLocalDateTime(raw: String?): LocalDateTime? {
        val v = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        // The UI's date picker emits "yyyy-MM-dd HH:mm:ss"; ISO format ("yyyy-MM-ddTHH:mm:ss") is
        // accepted as a fallback so a non-UI caller (curl, integration test) can use the standard
        // form without translation. Anything else fails fast at the controller layer rather than
        // silently producing the wrong window.
        return runCatching { LocalDateTime.parse(v, UI_FORMAT) }
            .recoverCatching { LocalDateTime.parse(v) }
            .getOrElse { throw IllegalArgumentException("Cannot parse datetime '$v'; expected 'yyyy-MM-dd HH:mm:ss' or ISO-8601") }
    }

    companion object {
        private val UI_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        private const val serialVersionUID = 1L
    }
}
