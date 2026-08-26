package io.kudos.ms.auth.core.authentication.securityevent.policy

import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskLevelEnum
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventRecordCommand
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration
import java.time.LocalDateTime

/** Configurable default SLA deadlines; replace [IAuthSecurityEventSlaPolicy] for industry-specific rules. */
@ConfigurationProperties(prefix = "kudos.ms.auth.security-event.sla")
open class AuthSecurityEventSlaProperties {
    var enabled: Boolean = true
    var notEvaluated: Duration = Duration.ofHours(24)
    var normal: Duration = Duration.ofHours(24)
    var warning: Duration = Duration.ofHours(4)
    var critical: Duration = Duration.ofHours(1)
}

fun interface IAuthSecurityEventSlaPolicy {
    /** Returns null only when this deployment intentionally disables SLA tracking for the event. */
    fun calculateDueAt(command: AuthSecurityEventRecordCommand): LocalDateTime?
}

open class DefaultAuthSecurityEventSlaPolicy(
    private val properties: AuthSecurityEventSlaProperties,
) : IAuthSecurityEventSlaPolicy {
    override fun calculateDueAt(command: AuthSecurityEventRecordCommand): LocalDateTime? {
        if (!properties.enabled) return null
        val duration = when (command.riskLevel) {
            WebAuthnAuthenticatorRiskLevelEnum.NOT_EVALUATED -> properties.notEvaluated
            WebAuthnAuthenticatorRiskLevelEnum.NORMAL -> properties.normal
            WebAuthnAuthenticatorRiskLevelEnum.WARNING -> properties.warning
            WebAuthnAuthenticatorRiskLevelEnum.CRITICAL -> properties.critical
        }
        require(!duration.isZero && !duration.isNegative) {
            "Authentication security event SLA durations must be positive"
        }
        return command.occurredAt.plus(duration)
    }
}

/** Default repeated escalation cadence after the initial SLA deadline is breached. */
@ConfigurationProperties(prefix = "kudos.ms.auth.security-event.escalation")
open class AuthSecurityEventEscalationProperties {
    var maxLevel: Int = 3
    var repeatInterval: Duration = Duration.ofHours(4)
}

fun interface IAuthSecurityEventEscalationPolicy {
    /** Returns the next due instant after [newLevel], or null when no further escalation is required. */
    fun nextEscalationAt(newLevel: Int, escalatedAt: LocalDateTime): LocalDateTime?
}

open class DefaultAuthSecurityEventEscalationPolicy(
    private val properties: AuthSecurityEventEscalationProperties,
) : IAuthSecurityEventEscalationPolicy {
    override fun nextEscalationAt(newLevel: Int, escalatedAt: LocalDateTime): LocalDateTime? {
        require(properties.maxLevel in 1..MAX_ESCALATION_LEVEL) {
            "Authentication security event max escalation level must be between 1 and $MAX_ESCALATION_LEVEL"
        }
        if (newLevel >= properties.maxLevel) return null
        require(!properties.repeatInterval.isZero && !properties.repeatInterval.isNegative) {
            "Authentication security event escalation repeat interval must be positive"
        }
        return escalatedAt.plus(properties.repeatInterval)
    }

    private companion object {
        const val MAX_ESCALATION_LEVEL = 10
    }
}
