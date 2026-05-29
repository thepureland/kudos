package io.kudos.ability.log.audit.rdb.clickhouse.service

import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.common.entity.SysAuditDetailLogVo
import io.kudos.ability.log.audit.common.entity.SysAuditLogModel
import io.kudos.ability.log.audit.common.entity.SysAuditLogVo
import io.kudos.ability.data.rdb.ktorm.datasource.currentDatabase
import io.kudos.ability.log.audit.rdb.clickhouse.table.SysAuditDetailLogTable
import io.kudos.ability.log.audit.rdb.clickhouse.table.SysAuditLogTable
import io.kudos.base.logger.LogFactory
import io.kudos.context.core.KudosContextHolder
import org.ktorm.dsl.AssignmentsBuilder
import org.ktorm.dsl.insert
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * ClickHouse-backed [IAuditService] implementation.
 *
 * Same wire-format / fallback rules as `RdbKtormAuditService`:
 *  - Empty model → `true` (no-op success).
 *  - Persistence failure → `false` (caller decides whether to retry or write to a local file).
 *  - Never throws. Audit pipeline failures must never break business flow.
 *  - tenantId / subSysCode use the entity-first-then-model fallback rule.
 *
 * Why **no** `@Transactional`:
 *  - ClickHouse does not support multi-statement transactions in the typical RDBMS sense
 *    (1) inserts into MergeTree become visible at part-merge time, (2) `BEGIN/COMMIT` are
 *    accepted but have no rollback semantics on data writes.
 *  - Adding `@Transactional` would make this code look atomic without actually being so, which
 *    is worse than the current "no annotation + we know it's not atomic" stance.
 *
 * Why **no** `REQUIRES_NEW`:
 *  - Same — no transaction boundary to break or share.
 *
 * Writes use **individual INSERTs in a loop**, not ktorm `batchInsert`. The JDBC `addBatch +
 * executeBatch` protocol is unreliable against ClickHouse JDBC for multi-row payloads (the
 * driver claims success but the rows never land), so we trade N round-trips for correctness.
 * Audit log volumes at the per-aspect call level are small (typically 1-3 entities per submit),
 * so the round-trip cost is acceptable. High-volume / fan-out deployments should declare a
 * custom service bean and use ClickHouse's bulk-insert protocols directly.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class RdbClickhouseAuditService : IAuditService {

    private val log = LogFactory.getLog(this::class)

    override fun submit(sysAuditLogVo: SysAuditLogModel): Boolean {
        val entities = sysAuditLogVo.entities.orEmpty().filterNotNull()
        val details = sysAuditLogVo.sysAuditDetailLogs.orEmpty().filterNotNull()
        if (entities.isEmpty() && details.isEmpty()) {
            log.debug("Audit log model is empty; skipping ClickHouse write")
            return true
        }
        return try {
            // Resolve the active Database per-call so the kudos dynamic-datasource layer can
            // route different tenants to different physical ClickHouse instances. Caching the
            // Database in a field would pin all writes to whichever DS was bound at bean-creation
            // time.
            val database = KudosContextHolder.currentDatabase()
            entities.forEach { entity ->
                database.insert(SysAuditLogTable) { applyAuditLog(this, entity, sysAuditLogVo) }
            }
            details.forEach { detail ->
                database.insert(SysAuditDetailLogTable) { applyDetailLog(this, detail) }
            }
            true
        } catch (t: Throwable) {
            log.error(
                t,
                "Failed to persist ClickHouse audit log tenant={0} subSys={1} entities={2} details={3}",
                sysAuditLogVo.tenantId, sysAuditLogVo.subSysCode, entities.size, details.size,
            )
            false
        }
    }

    private fun applyAuditLog(item: AssignmentsBuilder, entity: SysAuditLogVo, model: SysAuditLogModel) {
        with(SysAuditLogTable) {
            item.set(id, entity.id)
            item.set(entityId, entity.entityId)
            item.set(operateTypeId, entity.operateTypeId)
            item.set(operateType, entity.operateType)
            item.set(moduleId, entity.moduleId)
            item.set(moduleName, entity.moduleName)
            item.set(moduleCode, entity.moduleCode)
            item.set(description, entity.description)
            item.set(operator, entity.operator)
            item.set(operatorId, entity.operatorId)
            item.set(operatorUserType, entity.operatorUserType)
            item.set(tenantId, entity.tenantId ?: model.tenantId)
            item.set(sourceTenantId, entity.sourceTenantId)
            item.set(subSysCode, entity.subSysCode ?: model.subSysCode)
            item.set(operateTime, entity.operateTime?.toLocalDateTime() ?: LocalDateTime.now())
            item.set(operateIp, entity.operateIp)
            item.set(operateIpDictCode, entity.operateIpDictCode)
            item.set(clientOs, entity.clientOs)
            item.set(clientBrowser, entity.clientBrowser)
            item.set(requestType, entity.requestType)
        }
    }

    private fun applyDetailLog(item: AssignmentsBuilder, detail: SysAuditDetailLogVo) {
        with(SysAuditDetailLogTable) {
            item.set(id, detail.id)
            item.set(auditId, detail.auditId)
            item.set(operateUrl, detail.operateUrl)
            item.set(stringParams, detail.stringParams)
            item.set(objectParams, detail.objectParams)
            item.set(requestReferer, detail.requestReferer)
            item.set(requestFormData, detail.requestFormData)
            item.set(description, detail.description)
        }
    }

    private fun java.util.Date.toLocalDateTime(): LocalDateTime =
        LocalDateTime.ofInstant(this.toInstant(), ZoneId.systemDefault())
}
