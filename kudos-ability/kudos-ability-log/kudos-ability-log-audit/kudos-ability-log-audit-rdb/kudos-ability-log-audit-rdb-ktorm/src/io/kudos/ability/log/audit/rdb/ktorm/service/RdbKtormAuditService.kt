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
 * 审计日志的 RDB Ktorm 落地实现。
 *
 * 关键设计：
 *
 * 1. **不同时持久化主表 + 详情表**——业务侧的 `SysAuditLogModel` 已经把"主条目"与
 *    "详情条目"拆开放在 [SysAuditLogModel.entities] 与 [SysAuditLogModel.sysAuditDetailLogs]
 *    两个字段；本类各自批量 insert，避免做 N+1 query
 *
 * 2. **事务边界 `REQUIRES_NEW`**——审计动作不应该挂在业务事务里：业务事务回滚不应该
 *    带走审计记录（"我们想知道这个操作失败过"），同时审计失败也不应该撞翻业务事务
 *    （本类 `submit` catch 全部异常，外层调用方看到 `false` 即可）
 *
 * 3. **`tenantId` / `subSysCode` 兜底**——`SysAuditLogModel` 顶层带有 tenantId /
 *    subSysCode，但每条 entity 也可能各自带；优先用 entity 自身字段，缺失时用顶层兜底
 *
 * 4. **失败语义返回 false 而非抛出**——和 [io.kudos.ability.log.audit.mq.beans.MqAuditService]
 *    "永远返回 true"形成对比：RDB 路径是同步的，能感知 SQL 异常，所以可以如实回报
 *    "提交失败"，业务侧的切面可以据此决定是否兜底（如降级写本地文件）
 *
 * @author K
 * @since 1.0.0
 */
open class RdbKtormAuditService : IAuditService {

    private val log = LogFactory.getLog(this::class)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun submit(sysAuditLogVo: SysAuditLogModel): Boolean {
        val entities = sysAuditLogVo.entities.orEmpty().filterNotNull()
        val details = sysAuditLogVo.sysAuditDetailLogs.orEmpty().filterNotNull()
        if (entities.isEmpty() && details.isEmpty()) {
            log.debug("审计日志模型为空，跳过落库")
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
            log.error(t, "审计日志落库失败 tenant={0} subSys={1} entities={2} details={3}",
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

    /** `java.util.Date` → `java.time.LocalDateTime`，按 JVM 默认时区。 */
    private fun java.util.Date.toLocalDateTime(): LocalDateTime =
        LocalDateTime.ofInstant(this.toInstant(), ZoneId.systemDefault())
}
