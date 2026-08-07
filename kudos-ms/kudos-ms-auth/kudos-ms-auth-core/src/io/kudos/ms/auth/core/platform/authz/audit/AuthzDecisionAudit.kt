package io.kudos.ms.auth.core.platform.authz.audit

import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.common.authz.vo.AuthzDecision
import io.kudos.ms.auth.common.authz.vo.AuthzRequest
import io.kudos.ms.auth.core.platform.authz.init.properties.AuthzProperties
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Sanitised, immutable evidence emitted after a decision. Context values are deliberately omitted.
 *
 * @author K
 * @author AI: Codex
 */
data class AuthzDecisionAuditRecord(
    val occurredAt: LocalDateTime,
    val principalId: String,
    val principalType: String,
    val permissionCode: String,
    val resourceType: String?,
    val instanceId: String?,
    val contextKeys: Set<String>,
    val permitted: Boolean,
    val reason: AuthzDecision.Reason,
    val matchedCode: String?,
    val matchedRoleId: String?,
)

/**
 * Extension point for durable SIEM, database, or message-bus decision audit sinks.
 *
 * @author K
 * @author AI: Codex
 */
fun interface IAuthzDecisionAuditSink {
    fun record(record: AuthzDecisionAuditRecord)
}

/**
 * Applies audit policy once and fans a sanitised record out to every registered sink.
 *
 * @author K
 * @author AI: Codex
 */
@Component
open class AuthzDecisionAuditRecorder(
    private val sinks: ObjectProvider<IAuthzDecisionAuditSink>,
    private val properties: AuthzProperties,
) {
    private val log = LogFactory.getLog(this::class)

    open fun record(request: AuthzRequest, decision: AuthzDecision) {
        val selected = properties.auditAllDecisions ||
            (!decision.permitted && properties.auditDeniedDecisions) ||
            (decision.reason == AuthzDecision.Reason.PLATFORM_ADMIN && properties.auditPlatformAdminDecisions)
        if (!selected) return

        val record = AuthzDecisionAuditRecord(
            occurredAt = LocalDateTime.now(),
            principalId = request.subject.principalId,
            principalType = request.subject.principalType,
            permissionCode = request.permissionCode,
            resourceType = request.resourceType,
            instanceId = request.instanceId,
            contextKeys = request.context.keys.toSortedSet(),
            permitted = decision.permitted,
            reason = decision.reason,
            matchedCode = decision.matchedCode,
            matchedRoleId = decision.matchedRoleId,
        )
        sinks.orderedStream().forEach { sink ->
            try {
                sink.record(record)
            } catch (e: Exception) {
                log.error("Authorization decision audit sink ${sink.javaClass.name} failed: ${e.message}", e)
                if (properties.failOnAuditError) throw e
            }
        }
    }
}

/**
 * Baseline structured audit sink; deployments can add a durable sink through the SPI.
 *
 * @author K
 * @author AI: Codex
 */
@Component
open class LoggingAuthzDecisionAuditSink : IAuthzDecisionAuditSink {
    private val log = LogFactory.getLog(this::class)

    override fun record(record: AuthzDecisionAuditRecord) {
        log.info(
            "AUTHZ_DECISION principal=${record.principalType}:${record.principalId} " +
                "permission=${record.permissionCode} permitted=${record.permitted} reason=${record.reason} " +
                "resource=${record.resourceType ?: "-"}:${record.instanceId ?: "-"} " +
                "matchedCode=${record.matchedCode ?: "-"} matchedRole=${record.matchedRoleId ?: "-"} " +
                "contextKeys=${record.contextKeys}",
        )
    }
}
