package io.kudos.ability.log.audit.mongo.service

import io.kudos.ability.log.audit.common.api.IAuditLogReadOnlyService
import io.kudos.ability.log.audit.common.entity.AuditLogPage
import io.kudos.ability.log.audit.common.entity.AuditLogQuery
import io.kudos.ability.log.audit.common.entity.SysAuditDetailLogVo
import io.kudos.ability.log.audit.common.entity.SysAuditLogVo
import io.kudos.ability.log.audit.mongo.entity.SysAuditDetailLogDocument
import io.kudos.ability.log.audit.mongo.entity.SysAuditLogDocument
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.regex.Pattern

/**
 * Mongo implementation of [IAuditLogReadOnlyService] — the read-side companion to
 * [io.kudos.ability.log.audit.mongo.service.MongoAuditService].
 *
 * Key design points (matches the ktorm sibling impl, adapted for Mongo):
 *  - `findById(id)` reads the main document and maps to [SysAuditLogVo].
 *  - `findDetailById(auditId)` reads the **same main document** and surfaces its embedded
 *    [SysAuditLogDocument.detail] sub-field. Mongo stores detail nested under the audit; there is
 *    no separate `sys_audit_detail_log` collection in this module. Apps coming from the RDB world
 *    don't see a behavioral difference — the lookup still returns the matching detail or `null`.
 *  - `pagingSearch(...)` builds a [Criteria] from non-null [AuditLogQuery] fields, runs one
 *    `count()` for the page-control total, then a `find` with `skip + limit + sort`. Default
 *    sort is `operate_time DESC` to match the ktorm impl and the typical admin "newest first" view.
 *
 * `_Like` filters (`operatorLike` / `moduleCodeLike` / `descriptionLike`) translate to
 * case-insensitive substring regex. Mongo doesn't natively quote regex metacharacters so the
 * caller's input gets [Pattern.quote] to prevent injection of `.`, `*`, etc. — the resulting query
 * matches the literal substring just like a SQL `LIKE '%value%'` would.
 *
 * Ordering / read consistency:
 *  - This service does NOT participate in Spring transactions; `MongoTemplate` reads are
 *    naturally consistent against a primary replica, and standalone Mongo has no per-statement
 *    snapshot to share.
 *  - Pagination uses skip + limit, which is O(skip + limit) per Mongo's `find` semantics. The
 *    high-watermark trade-off the ktorm impl makes — accepting unbounded `pagingSearch` for
 *    first-port simplicity — applies here too; very deep page numbers will get slow.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
open class MongoAuditLogReadOnlyService(
    private val mongoTemplate: MongoTemplate,
) : IAuditLogReadOnlyService {

    override fun findById(id: String): SysAuditLogVo? {
        return mongoTemplate.findById(id, SysAuditLogDocument::class.java)?.toVo()
    }

    override fun findDetailById(auditId: String): SysAuditDetailLogVo? {
        val parent = mongoTemplate.findById(auditId, SysAuditLogDocument::class.java) ?: return null
        return parent.detail?.toVo()
    }

    override fun pagingSearch(query: AuditLogQuery, pageNo: Int, pageSize: Int): AuditLogPage {
        val safePageNo = pageNo.coerceAtLeast(1)
        val safePageSize = pageSize.coerceAtLeast(1)

        val criteria = buildCriteria(query)
        val countQuery = Query.query(criteria)
        val total = mongoTemplate.count(countQuery, SysAuditLogDocument::class.java)
        if (total == 0L) return AuditLogPage.empty(safePageNo, safePageSize)

        val findQuery = Query.query(criteria)
            .with(Sort.by(Sort.Direction.DESC, "operateTime"))
            .skip(((safePageNo - 1) * safePageSize).toLong())
            .limit(safePageSize)
        val docs = mongoTemplate.find(findQuery, SysAuditLogDocument::class.java)
        return AuditLogPage(
            items = docs.map { it.toVo() },
            total = total,
            pageNo = safePageNo,
            pageSize = safePageSize,
        )
    }

    /**
     * Build a Mongo [Criteria] AND-combining non-null [AuditLogQuery] fields. Empty `criteria` is
     * the all-match identity, so the search runs across the whole collection when no filter is
     * provided — matches the ktorm impl's "no filter → match all" stance.
     */
    private fun buildCriteria(query: AuditLogQuery): Criteria {
        val criteria = Criteria()
        val crumbs = mutableListOf<Criteria>()
        query.tenantId?.let { crumbs += Criteria.where("tenantId").`is`(it) }
        query.sourceTenantId?.let { crumbs += Criteria.where("sourceTenantId").`is`(it) }
        query.subSysCode?.let { crumbs += Criteria.where("subSysCode").`is`(it) }
        query.moduleCode?.let { crumbs += Criteria.where("moduleCode").`is`(it) }
        query.operateTypeId?.let { crumbs += Criteria.where("operateTypeId").`is`(it) }
        query.operatorId?.let { crumbs += Criteria.where("operatorId").`is`(it) }
        query.operatorUserType?.let { crumbs += Criteria.where("operatorUserType").`is`(it) }
        query.operatorLike?.takeIf { it.isNotEmpty() }?.let {
            crumbs += Criteria.where("operator").regex(quoteSubstring(it), "i")
        }
        query.moduleCodeLike?.takeIf { it.isNotEmpty() }?.let {
            crumbs += Criteria.where("moduleCode").regex(quoteSubstring(it), "i")
        }
        query.operateType?.takeIf { it.isNotEmpty() }?.let {
            crumbs += Criteria.where("operateType").`is`(it)
        }
        query.entityId?.let { crumbs += Criteria.where("entityId").`is`(it) }
        // operate_time bounds: [from, to) — inclusive lower, exclusive upper (matches ktorm).
        if (query.operateTimeFrom != null || query.operateTimeTo != null) {
            val timeCriteria = Criteria.where("operateTime")
            query.operateTimeFrom?.let { timeCriteria.gte(it.toDate()) }
            query.operateTimeTo?.let { timeCriteria.lt(it.toDate()) }
            crumbs += timeCriteria
        }
        query.descriptionLike?.takeIf { it.isNotEmpty() }?.let {
            crumbs += Criteria.where("description").regex(quoteSubstring(it), "i")
        }
        return if (crumbs.isEmpty()) criteria else criteria.andOperator(*crumbs.toTypedArray())
    }

    /**
     * `LIKE '%value%'` substring-match semantics in regex form. [Pattern.quote] wraps the input in
     * `\Q...\E` so caller-supplied metacharacters (`.`, `*`, `[`, etc.) match literally — no risk
     * of a stray query field acting as a wildcard or as a regex injection vector.
     */
    private fun quoteSubstring(value: String): String = Pattern.quote(value)

    private fun SysAuditLogDocument.toVo(): SysAuditLogVo = SysAuditLogVo().also { vo ->
        vo.id = id
        vo.entityId = entityId
        vo.operateTypeId = operateTypeId
        vo.operateType = operateType
        vo.moduleName = moduleName
        vo.moduleCode = moduleCode
        vo.moduleId = moduleId
        vo.description = description
        vo.operator = operator
        vo.tenantId = tenantId
        vo.sourceTenantId = sourceTenantId
        vo.subSysCode = subSysCode
        vo.operateTime = operateTime
        vo.operateIp = operateIp
        vo.operateIpDictCode = operateIpDictCode
        vo.operatorId = operatorId
        vo.operatorUserType = operatorUserType
        vo.clientOs = clientOs
        vo.clientBrowser = clientBrowser
        vo.requestType = requestType
    }

    private fun SysAuditDetailLogDocument.toVo(): SysAuditDetailLogVo = SysAuditDetailLogVo().also { vo ->
        vo.id = id
        vo.auditId = auditId
        vo.operateUrl = operateUrl
        vo.stringParams = stringParams
        vo.objectParams = objectParams
        vo.requestReferer = requestReferer
        vo.requestFormData = requestFormData
        vo.description = description
    }

    private fun LocalDateTime.toDate(): Date =
        Date.from(this.atZone(ZoneId.systemDefault()).toInstant())
}
