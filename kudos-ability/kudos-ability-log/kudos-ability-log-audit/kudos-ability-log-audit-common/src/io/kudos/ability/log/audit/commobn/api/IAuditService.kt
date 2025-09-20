package io.kudos.ability.log.audit.commobn.api

import io.kudos.ability.log.audit.commobn.entity.SysAuditLogModel


interface IAuditService {
    /**
     * 审计日志生产方法
     *
     * @param sysAuditLogVo
     */
    fun submit(sysAuditLogVo: SysAuditLogModel): Boolean
}
