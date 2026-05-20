package io.kudos.ability.log.audit.common.api

import io.kudos.ability.log.audit.common.entity.SysAuditLogModel


/**
 * 审计日志生产接口：[LogAuditAspect] / [WebLogAuditAspect] 在拦截到 @Audit 方法后调本接口落盘。
 *
 * 实现方按存储后端不同分化：RDB 走 ktorm（见 `RdbKtormAuditService`）、MQ 走 stream binder 异步堆积。
 *
 * @author K
 * @since 1.0.0
 */
interface IAuditService {
    /**
     * 审计日志生产方法
     *
     * @param sysAuditLogVo
     */
    fun submit(sysAuditLogVo: SysAuditLogModel): Boolean
}
