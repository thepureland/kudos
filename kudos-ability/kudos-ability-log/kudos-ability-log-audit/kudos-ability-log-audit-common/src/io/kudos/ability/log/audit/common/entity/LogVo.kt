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

    /**
     * JSON property names whose value must be desensitized before the request body is persisted to
     * the audit trail. Populated by
     * [io.kudos.ability.log.audit.common.support.AuditLogTool.applyRequestDesensitizeFromFirstJoinPointArg]
     * by scanning `@LogDesensitize`-marked fields on the controller method's first parameter DTO.
     *
     * Carried on the LogVo so the `before` advice (which sees the JoinPoint) can collect the names
     * once, and the `afterReturning` advice (which actually masks the captured body) can apply them
     * without re-scanning reflection on the hot path.
     *
     * `null` or empty means "no masking required" — the body is written verbatim.
     */
    var requestDesensitizePropertyNames: Set<String>? = null

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
