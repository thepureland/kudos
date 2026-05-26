package io.kudos.ability.log.audit.common.api

import io.kudos.ability.log.audit.common.entity.SysAuditLogModel


/**
 * Audit-log producer interface: [LogAuditAspect] / [WebLogAuditAspect] call this interface to persist after
 * intercepting an @Audit method.
 *
 * Implementations differ by storage backend: RDB goes through ktorm (see `RdbKtormAuditService`), MQ goes through the
 * stream binder asynchronously.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IAuditService {
    /**
     * Audit-log producer method
     *
     * @param sysAuditLogVo
     */
    fun submit(sysAuditLogVo: SysAuditLogModel): Boolean
}
