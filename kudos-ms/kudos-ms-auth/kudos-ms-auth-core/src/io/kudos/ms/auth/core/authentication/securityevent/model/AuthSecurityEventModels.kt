package io.kudos.ms.auth.core.authentication.securityevent.model

import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskLevelEnum
import java.time.LocalDateTime

enum class AuthSecurityEventTypeEnum {
    WEBAUTHN_AUTHENTICATOR_RISK_BLOCKED,
}

enum class AuthSecurityEventStatusEnum {
    OPEN,
    ACKNOWLEDGED,
    CLOSED,
}

enum class AuthSecurityEventResolutionEnum {
    MITIGATED,
    FALSE_POSITIVE,
    ACCEPTED_RISK,
    DUPLICATE,
    OTHER,
}

data class AuthSecurityEventRecordCommand(
    val tenantId: String,
    val userId: String,
    val eventType: AuthSecurityEventTypeEnum,
    val subjectType: String,
    val subjectFingerprint: String,
    val riskLevel: WebAuthnAuthenticatorRiskLevelEnum,
    val riskSources: Set<String>,
    val riskStatusCodes: Set<String>,
    val deduplicationKey: String,
    val bucketStart: LocalDateTime,
    val occurredAt: LocalDateTime,
)

data class AuthSecurityEventAcknowledgeCommand(
    val tenantId: String,
    val eventId: String,
    val actorUserId: String,
    val reason: String,
    val expectedVersion: Long,
)

data class AuthSecurityEventCloseCommand(
    val tenantId: String,
    val eventId: String,
    val actorUserId: String,
    val resolution: String,
    val reason: String,
    val expectedVersion: Long,
)

data class AuthSecurityEventAssignCommand(
    val tenantId: String,
    val eventId: String,
    val actorUserId: String,
    val assigneeUserId: String,
    val reason: String,
    val expectedVersion: Long,
)

data class AuthSecurityEventSummary(
    val id: String,
    val eventType: AuthSecurityEventTypeEnum,
    val userId: String,
    val subjectType: String,
    val subjectFingerprint: String,
    val riskLevel: WebAuthnAuthenticatorRiskLevelEnum,
    val riskSources: Set<String>,
    val riskStatusCodes: Set<String>,
    val status: AuthSecurityEventStatusEnum,
    val workflowVersion: Long,
    val occurrenceCount: Long,
    val bucketStart: LocalDateTime,
    val firstOccurredAt: LocalDateTime,
    val lastOccurredAt: LocalDateTime,
    val acknowledgedBy: String?,
    val acknowledgedAt: LocalDateTime?,
    val acknowledgeReason: String?,
    val resolution: AuthSecurityEventResolutionEnum?,
    val closedBy: String?,
    val closedAt: LocalDateTime?,
    val closeReason: String?,
    val assignedTo: String?,
    val assignedBy: String?,
    val assignedAt: LocalDateTime?,
    val assignmentReason: String?,
    val dueAt: LocalDateTime?,
    val escalationLevel: Int,
    val lastEscalatedAt: LocalDateTime?,
    val nextEscalationAt: LocalDateTime?,
)

class AuthSecurityEventException(
    val errorCode: String,
    cause: Throwable? = null,
) : IllegalArgumentException(errorCode, cause)
