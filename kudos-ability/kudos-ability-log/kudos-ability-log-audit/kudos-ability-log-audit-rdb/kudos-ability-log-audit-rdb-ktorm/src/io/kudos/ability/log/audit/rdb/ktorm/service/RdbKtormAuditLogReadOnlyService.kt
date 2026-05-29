package io.kudos.ability.log.audit.rdb.ktorm.service

import io.kudos.ability.data.rdb.ktorm.datasource.currentDatabase
import io.kudos.ability.log.audit.common.api.IAuditLogReadOnlyService
import io.kudos.ability.log.audit.common.entity.AuditLogPage
import io.kudos.ability.log.audit.common.entity.AuditLogQuery
import io.kudos.ability.log.audit.common.entity.SysAuditDetailLogVo
import io.kudos.ability.log.audit.common.entity.SysAuditLogVo
import io.kudos.ability.log.audit.rdb.ktorm.table.SysAuditDetailLogTable
import io.kudos.ability.log.audit.rdb.ktorm.table.SysAuditLogTable
import io.kudos.context.core.KudosContextHolder
import org.ktorm.dsl.QuerySource
import org.ktorm.dsl.and
import org.ktorm.dsl.desc
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.greaterEq
import org.ktorm.dsl.isNotNull
import org.ktorm.dsl.less
import org.ktorm.dsl.like
import org.ktorm.dsl.limit
import org.ktorm.dsl.map
import org.ktorm.dsl.orderBy
import org.ktorm.dsl.select
import org.ktorm.dsl.where
import org.ktorm.schema.ColumnDeclaring
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId
import java.util.Date

/**
 * Ktorm implementation of [IAuditLogReadOnlyService].
 *
 * **Routes via [KudosContextHolder.currentDatabase]** so multi-tenant deployments that pin audit
 * writes to a per-tenant datasource also resolve reads against the right database — the read path
 * inherits whichever routing decision the calling context already made.
 *
 * **Read-only transaction**: `@Transactional(readOnly = true, propagation = SUPPORTS)` lets the
 * call participate in any ambient transaction the admin controller may already have opened (e.g. a
 * managed UI session) without opening a new one when none exists.
 *
 * **Ordering**: every paged search returns rows by `operate_time DESC` so the admin "latest
 * activity first" view doesn't have to specify a sort. Soul's MyBatis impl exposes ORDER BY in the
 * mapper xml; we hard-code it here because every consumer wants the same shape — making it
 * configurable would just add a knob no one would change.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
open class RdbKtormAuditLogReadOnlyService : IAuditLogReadOnlyService {

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    override fun findById(id: String): SysAuditLogVo? {
        val db = KudosContextHolder.currentDatabase()
        return db.from(SysAuditLogTable)
            .select()
            .where { SysAuditLogTable.id eq id }
            .limit(0, 1)
            .map(::rowToVo)
            .firstOrNull()
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    override fun findDetailById(auditId: String): SysAuditDetailLogVo? {
        val db = KudosContextHolder.currentDatabase()
        return db.from(SysAuditDetailLogTable)
            .select()
            .where { SysAuditDetailLogTable.auditId eq auditId }
            .limit(0, 1)
            .map(::rowToDetailVo)
            .firstOrNull()
    }

    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    override fun pagingSearch(query: AuditLogQuery, pageNo: Int, pageSize: Int): AuditLogPage {
        val safePageNo = pageNo.coerceAtLeast(1)
        val safePageSize = pageSize.coerceAtLeast(1)

        val db = KudosContextHolder.currentDatabase()
        val baseQuery: QuerySource = db.from(SysAuditLogTable)

        val total: Long = baseQuery
            .select(SysAuditLogTable.id)
            .where { buildPredicate(query) }
            .totalRecordsInAllPages.toLong()
        if (total == 0L) return AuditLogPage.empty(safePageNo, safePageSize)

        val offset = (safePageNo - 1) * safePageSize
        val items: List<SysAuditLogVo> = baseQuery
            .select()
            .where { buildPredicate(query) }
            .orderBy(SysAuditLogTable.operateTime.desc())
            .limit(offset, safePageSize)
            .map(::rowToVo)

        return AuditLogPage(items = items, total = total, pageNo = safePageNo, pageSize = safePageSize)
    }

    /**
     * Composes the WHERE clause from non-null [AuditLogQuery] fields. Returns a constant `1=1`-ish
     * predicate (`id IS NOT NULL`) when no filter applies so the query still runs — preferable to
     * mutating the query with a no-op `.where { }` chain.
     */
    private fun buildPredicate(query: AuditLogQuery): ColumnDeclaring<Boolean> {
        val predicates: MutableList<ColumnDeclaring<Boolean>> = mutableListOf()
        query.tenantId?.let { predicates += SysAuditLogTable.tenantId eq it }
        query.sourceTenantId?.let { predicates += SysAuditLogTable.sourceTenantId eq it }
        query.subSysCode?.let { predicates += SysAuditLogTable.subSysCode eq it }
        query.moduleCode?.let { predicates += SysAuditLogTable.moduleCode eq it }
        query.operateTypeId?.let { predicates += SysAuditLogTable.operateTypeId eq it }
        query.operatorId?.let { predicates += SysAuditLogTable.operatorId eq it }
        query.operatorUserType?.let { predicates += SysAuditLogTable.operatorUserType eq it }
        query.operatorLike?.takeIf { it.isNotEmpty() }?.let {
            predicates += SysAuditLogTable.operator like "%$it%"
        }
        query.moduleCodeLike?.takeIf { it.isNotEmpty() }?.let {
            predicates += SysAuditLogTable.moduleCode like "%$it%"
        }
        query.operateType?.takeIf { it.isNotEmpty() }?.let {
            predicates += SysAuditLogTable.operateType eq it
        }
        query.entityId?.let { predicates += SysAuditLogTable.entityId eq it }
        query.operateTimeFrom?.let { predicates += SysAuditLogTable.operateTime greaterEq it }
        query.operateTimeTo?.let { predicates += SysAuditLogTable.operateTime less it }
        query.descriptionLike?.takeIf { it.isNotEmpty() }?.let {
            predicates += SysAuditLogTable.description like "%$it%"
        }
        if (predicates.isEmpty()) {
            // No filter ↔ "match all". Use an always-true predicate to keep the WHERE clause valid.
            return SysAuditLogTable.id.isNotNull()
        }
        return predicates.reduce { acc, p -> acc and p }
    }

    private fun rowToVo(row: org.ktorm.dsl.QueryRowSet): SysAuditLogVo = SysAuditLogVo().apply {
        id = row[SysAuditLogTable.id]
        entityId = row[SysAuditLogTable.entityId]
        operateTypeId = row[SysAuditLogTable.operateTypeId]
        operateType = row[SysAuditLogTable.operateType]
        moduleId = row[SysAuditLogTable.moduleId]
        moduleName = row[SysAuditLogTable.moduleName]
        moduleCode = row[SysAuditLogTable.moduleCode]
        description = row[SysAuditLogTable.description]
        operator = row[SysAuditLogTable.operator]
        operatorId = row[SysAuditLogTable.operatorId]
        operatorUserType = row[SysAuditLogTable.operatorUserType]
        tenantId = row[SysAuditLogTable.tenantId]
        sourceTenantId = row[SysAuditLogTable.sourceTenantId]
        subSysCode = row[SysAuditLogTable.subSysCode]
        operateTime = row[SysAuditLogTable.operateTime]?.toDate()
        operateIp = row[SysAuditLogTable.operateIp]
        operateIpDictCode = row[SysAuditLogTable.operateIpDictCode]
        clientOs = row[SysAuditLogTable.clientOs]
        clientBrowser = row[SysAuditLogTable.clientBrowser]
        requestType = row[SysAuditLogTable.requestType]
    }

    private fun rowToDetailVo(row: org.ktorm.dsl.QueryRowSet): SysAuditDetailLogVo = SysAuditDetailLogVo().apply {
        id = row[SysAuditDetailLogTable.id]
        auditId = row[SysAuditDetailLogTable.auditId]
        operateUrl = row[SysAuditDetailLogTable.operateUrl]
        stringParams = row[SysAuditDetailLogTable.stringParams]
        objectParams = row[SysAuditDetailLogTable.objectParams]
        requestReferer = row[SysAuditDetailLogTable.requestReferer]
        requestFormData = row[SysAuditDetailLogTable.requestFormData]
        description = row[SysAuditDetailLogTable.description]
    }

    /** `LocalDateTime` → legacy `java.util.Date` to preserve `SysAuditLogVo.operateTime` shape. */
    private fun java.time.LocalDateTime.toDate(): Date =
        Date.from(this.atZone(ZoneId.systemDefault()).toInstant())
}
