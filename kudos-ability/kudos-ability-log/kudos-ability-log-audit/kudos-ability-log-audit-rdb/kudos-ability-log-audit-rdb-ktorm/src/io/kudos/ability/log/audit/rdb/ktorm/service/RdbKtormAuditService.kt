package io.kudos.ability.log.audit.rdb.ktorm.service

import io.kudos.ability.data.rdb.ktorm.datasource.currentDatabase
import io.kudos.ability.log.audit.common.api.IAuditService
import io.kudos.ability.log.audit.common.entity.SysAuditDetailLogVo
import io.kudos.ability.log.audit.common.entity.SysAuditLogModel
import io.kudos.ability.log.audit.common.entity.SysAuditLogVo
import io.kudos.ability.log.audit.rdb.ktorm.table.SysAuditDetailLogTable
import io.kudos.ability.log.audit.rdb.ktorm.table.SysAuditLogTable
import io.kudos.base.logger.LogFactory
import io.kudos.context.core.KudosContextHolder
import org.ktorm.dsl.batchInsert
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * RDB Ktorm persistence implementation for audit logs.
 *
 * Key design points:
 *
 * 1. **Persists the main table and detail table separately** — the business-side `SysAuditLogModel` already splits
 *    "main entries" and "detail entries" into [SysAuditLogModel.entities] and [SysAuditLogModel.sysAuditDetailLogs];
 *    this class batch-inserts each side, avoiding N+1 queries.
 *
 * 2. **Transaction boundary `REQUIRES_NEW`** — audit actions should not be attached to the business transaction:
 *    a business rollback should not take audit records with it ("we want to know this operation failed"), and
 *    similarly an audit failure should not bring down the business transaction (`submit` here catches all exceptions,
 *    callers just see `false`).
 *
 * 3. **`tenantId` / `subSysCode` fallback** — `SysAuditLogModel` carries top-level tenantId / subSysCode, but each
 *    entity may also carry its own; prefer the entity's own field, fall back to the top-level when missing.
 *
 * 4. **Failure returns false instead of throwing** — contrast with [io.kudos.ability.log.audit.mq.beans.MqAuditService]
 *    which "always returns true": the RDB path is synchronous and can observe SQL exceptions, so it honestly reports
 *    "submit failed", letting the business-side aspect decide on fallbacks (e.g., degrade to writing to a local file).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class RdbKtormAuditService : IAuditService {

    private val log = LogFactory.getLog(this::class)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun submit(sysAuditLogVo: SysAuditLogModel): Boolean {
        val entities = sysAuditLogVo.entities.orEmpty().filterNotNull()
        val details = sysAuditLogVo.sysAuditDetailLogs.orEmpty().filterNotNull()
        if (entities.isEmpty() && details.isEmpty()) {
            log.debug("Audit log model is empty, skipping persistence")
            return true
        }
        return try {
            val db = KudosContextHolder.currentDatabase()
            if (entities.isNotEmpty()) {
                db.batchInsert(SysAuditLogTable) {
                    entities.forEach { entity ->
                        item {
                            applyAuditLog(this, entity, sysAuditLogVo)
                        }
                    }
                }
            }
            if (details.isNotEmpty()) {
                db.batchInsert(SysAuditDetailLogTable) {
                    details.forEach { detail ->
                        item {
                            applyDetailLog(this, detail)
                        }
                    }
                }
            }
            true
        } catch (t: Throwable) {
            log.error(t, "Failed to persist audit log tenant={0} subSys={1} entities={2} details={3}",
                sysAuditLogVo.tenantId, sysAuditLogVo.subSysCode, entities.size, details.size)
            false
        }
    }

    /**
     * 把 [SysAuditLogVo] 各字段写入 ktorm batch insert 的 [AssignmentsBuilder]。
     *
     * `tenantId` / `subSysCode` 两个字段优先取 entity 自带值；为空时回退到 [SysAuditLogModel] 上下文级别的值——
     * 兼容业务侧只在 model 级标注 tenant/subSys 的简写场景。
     *
     * @param item ktorm 赋值构造器
     * @param entity 单条审计记录
     * @param model 整批审计模型（提供上下文 tenant / subSys 回退）
     * @author K
     * @since 1.0.0
     */
    private fun applyAuditLog(
        item: org.ktorm.dsl.AssignmentsBuilder,
        entity: SysAuditLogVo,
        model: SysAuditLogModel,
    ) {
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

    /**
     * 把 [SysAuditDetailLogVo] 各字段写入 ktorm batch insert 的 [AssignmentsBuilder]。
     * 与 [applyAuditLog] 分开是因为审计详情和主审计是两张表，sub-insert 走不同 builder。
     *
     * @param item ktorm 赋值构造器
     * @param detail 单条详情记录
     * @author K
     * @since 1.0.0
     */
    private fun applyDetailLog(
        item: org.ktorm.dsl.AssignmentsBuilder,
        detail: SysAuditDetailLogVo,
    ) {
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

    /** `java.util.Date` → `java.time.LocalDateTime`, using the JVM default zone. */
    private fun java.util.Date.toLocalDateTime(): LocalDateTime =
        LocalDateTime.ofInstant(this.toInstant(), ZoneId.systemDefault())
}
