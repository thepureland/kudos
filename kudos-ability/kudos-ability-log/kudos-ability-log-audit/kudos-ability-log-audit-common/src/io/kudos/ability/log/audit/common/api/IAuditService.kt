package io.kudos.ability.log.audit.common.api

import io.kudos.ability.log.audit.common.entity.SysAuditLogModel


interface IAuditService {
    /**
     * 审计日志生产方法
     *
     * @param sysAuditLogVo
     */
    fun submit(sysAuditLogVo: SysAuditLogModel): Boolean
}
