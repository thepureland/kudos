package io.kudos.ability.log.audit.common.entity

import io.kudos.ability.log.audit.common.annotation.Audit
import io.kudos.ability.log.audit.common.annotation.WebAudit
import io.kudos.ability.log.audit.common.support.ILogVo


/**
 * Top-level audit-log container: a single method call may produce N [BaseLog] entries (related records across
 * different subsystems / modules).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class LogVo : ILogVo {

    /** Audit-log entries accumulated for the current method call. */
    var logs = mutableListOf<BaseLog>()

    /** Appends a log entry derived from the [WebAudit] annotation. */
    fun addAuditLog(audit: WebAudit): BaseLog {
        val sysLog = BaseLog(audit)
        this.logs.add(sysLog)
        return sysLog
    }

    /** Appends a log entry derived from the [Audit] annotation. */
    fun addAuditLog(audit: Audit): BaseLog {
        val sysLog = BaseLog(audit)
        this.logs.add(sysLog)
        return sysLog
    }

    companion object {
        private const val serialVersionUID = -6940790149742441845L
    }
}
