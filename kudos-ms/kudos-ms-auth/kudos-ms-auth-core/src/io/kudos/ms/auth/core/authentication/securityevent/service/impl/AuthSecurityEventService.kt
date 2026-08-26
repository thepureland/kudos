package io.kudos.ms.auth.core.authentication.securityevent.service.impl

import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskLevelEnum
import io.kudos.ms.auth.core.authentication.securityevent.dao.AuthSecurityEventDao
import io.kudos.ms.auth.core.authentication.securityevent.event.AuthSecurityEventAssigned
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventAcknowledgeCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventAssignCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventCloseCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventRecordCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventResolutionEnum
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventStatusEnum
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventSummary
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventTypeEnum
import io.kudos.ms.auth.core.authentication.securityevent.model.po.AuthSecurityEvent
import io.kudos.ms.auth.core.authentication.securityevent.policy.IAuthSecurityEventSlaPolicy
import io.kudos.ms.auth.core.authentication.securityevent.service.iservice.IAuthSecurityEventService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
open class AuthSecurityEventService(
    private val dao: AuthSecurityEventDao,
    private val slaPolicy: IAuthSecurityEventSlaPolicy,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock = Clock.systemUTC(),
) : IAuthSecurityEventService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun recordOrAggregate(command: AuthSecurityEventRecordCommand) {
        validate(command)
        val now = LocalDateTime.now(clock)
        if (dao.incrementOccurrence(command, now)) return
        dao.insert(command.toEntity(now, slaPolicy.calculateDueAt(command)))
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun aggregateAfterConcurrentInsert(command: AuthSecurityEventRecordCommand) {
        validate(command)
        if (!dao.incrementOccurrence(command, LocalDateTime.now(clock))) {
            fail("AUTH_SECURITY_EVENT_CONCURRENT_RECORD_NOT_FOUND")
        }
    }

    @Transactional
    override fun acknowledge(command: AuthSecurityEventAcknowledgeCommand): AuthSecurityEventSummary {
        val reason = validateWorkflowCommand(
            command.tenantId,
            command.eventId,
            command.actorUserId,
            command.reason,
            command.expectedVersion,
        )
        val current = findForWorkflow(command.tenantId, command.eventId)
        val now = LocalDateTime.now(clock)
        if (now.isBefore(current.bucketStart.plusMinutes(DEDUPLICATION_BUCKET_MINUTES.toLong()))) {
            fail("AUTH_SECURITY_EVENT_BUCKET_NOT_SETTLED")
        }
        requireWorkflowState(current, AuthSecurityEventStatusEnum.OPEN, command.expectedVersion)
        if (!dao.acknowledge(
                command.tenantId,
                command.eventId,
                command.expectedVersion,
                command.actorUserId,
                reason,
                now,
            )
        ) {
            fail("AUTH_SECURITY_EVENT_TRANSITION_CONFLICT")
        }
        return findForWorkflow(command.tenantId, command.eventId).toSummary()
    }

    @Transactional
    override fun close(command: AuthSecurityEventCloseCommand): AuthSecurityEventSummary {
        val reason = validateWorkflowCommand(
            command.tenantId,
            command.eventId,
            command.actorUserId,
            command.reason,
            command.expectedVersion,
        )
        val resolution = parseResolution(command.resolution)
        val current = findForWorkflow(command.tenantId, command.eventId)
        requireWorkflowState(current, AuthSecurityEventStatusEnum.ACKNOWLEDGED, command.expectedVersion)
        val now = LocalDateTime.now(clock)
        if (!dao.close(
                command.tenantId,
                command.eventId,
                command.expectedVersion,
                command.actorUserId,
                resolution.name,
                reason,
                now,
            )
        ) {
            fail("AUTH_SECURITY_EVENT_TRANSITION_CONFLICT")
        }
        return findForWorkflow(command.tenantId, command.eventId).toSummary()
    }

    @Transactional
    override fun assign(command: AuthSecurityEventAssignCommand): AuthSecurityEventSummary {
        val reason = validateWorkflowCommand(
            command.tenantId,
            command.eventId,
            command.actorUserId,
            command.reason,
            command.expectedVersion,
        )
        requireIdentifier(command.assigneeUserId, "AUTH_SECURITY_EVENT_ASSIGNEE_INVALID")
        val current = findForWorkflow(command.tenantId, command.eventId)
        requireWorkflowVersion(current, command.expectedVersion)
        if (parseStatus(current.status) == AuthSecurityEventStatusEnum.CLOSED) {
            fail("AUTH_SECURITY_EVENT_STATE_CONFLICT")
        }
        val now = LocalDateTime.now(clock)
        if (!dao.assign(
                tenantId = command.tenantId,
                eventId = command.eventId,
                expectedVersion = command.expectedVersion,
                actorUserId = command.actorUserId,
                assigneeUserId = command.assigneeUserId,
                reason = reason,
                occurredAt = now,
            )
        ) {
            fail("AUTH_SECURITY_EVENT_TRANSITION_CONFLICT")
        }
        val assigned = findForWorkflow(command.tenantId, command.eventId)
        publishAfterCommit(
            AuthSecurityEventAssigned(
                tenantId = command.tenantId,
                eventId = command.eventId,
                actorUserId = command.actorUserId,
                assigneeUserId = command.assigneeUserId,
                previousAssigneeUserId = current.assignedTo,
                dueAt = assigned.dueAt,
                occurredAt = now,
            )
        )
        return assigned.toSummary()
    }

    private fun publishAfterCommit(event: Any) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publishSafely(event)
            return
        }
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() = publishSafely(event)
            }
        )
    }

    private fun publishSafely(event: Any) {
        runCatching { eventPublisher.publishEvent(event) }
            .onFailure { logger.error("Failed to publish authentication security event notification", it) }
    }

    @Transactional(readOnly = true)
    override fun listRecent(
        tenantId: String,
        userId: String?,
        riskLevel: String?,
        status: String?,
        assigneeUserId: String?,
        overdueOnly: Boolean,
        limit: Int,
    ): List<AuthSecurityEventSummary> {
        requireIdentifier(tenantId, "AUTH_SECURITY_EVENT_TENANT_INVALID")
        userId?.let { requireIdentifier(it, "AUTH_SECURITY_EVENT_USER_INVALID") }
        assigneeUserId?.let { requireIdentifier(it, "AUTH_SECURITY_EVENT_ASSIGNEE_INVALID") }
        if (limit !in 1..MAX_QUERY_LIMIT) fail("AUTH_SECURITY_EVENT_LIMIT_INVALID")
        val normalizedRiskLevel = riskLevel?.takeIf { it.isNotBlank() }?.let(::parseRiskLevel)?.name
        val normalizedStatus = status?.takeIf { it.isNotBlank() }?.let(::parseStatus)?.name
        val overdueBefore = LocalDateTime.now(clock).takeIf { overdueOnly }
        return dao.findRecent(
            tenantId,
            userId,
            normalizedRiskLevel,
            normalizedStatus,
            assigneeUserId,
            overdueBefore,
            limit,
        ).map { it.toSummary() }
    }

    private fun validate(command: AuthSecurityEventRecordCommand) {
        requireIdentifier(command.tenantId, "AUTH_SECURITY_EVENT_TENANT_INVALID")
        requireIdentifier(command.userId, "AUTH_SECURITY_EVENT_USER_INVALID")
        if (!CODE.matches(command.subjectType)) fail("AUTH_SECURITY_EVENT_SUBJECT_TYPE_INVALID")
        if (!FINGERPRINT.matches(command.subjectFingerprint)) fail("AUTH_SECURITY_EVENT_SUBJECT_INVALID")
        if (!FINGERPRINT.matches(command.deduplicationKey)) fail("AUTH_SECURITY_EVENT_DEDUPLICATION_KEY_INVALID")
        if (
            command.bucketStart.minute % DEDUPLICATION_BUCKET_MINUTES != 0 ||
            command.bucketStart.second != 0 ||
            command.bucketStart.nano != 0
        ) {
            fail("AUTH_SECURITY_EVENT_BUCKET_INVALID")
        }
        if (
            command.occurredAt < command.bucketStart ||
            !command.occurredAt.isBefore(command.bucketStart.plusMinutes(DEDUPLICATION_BUCKET_MINUTES.toLong()))
        ) {
            fail("AUTH_SECURITY_EVENT_OCCURRED_AT_INVALID")
        }
        if (
            command.riskSources.size > MAX_CODES_PER_DIMENSION ||
            command.riskStatusCodes.size > MAX_CODES_PER_DIMENSION
        ) {
            fail("AUTH_SECURITY_EVENT_RISK_CODE_LIMIT_EXCEEDED")
        }
        if (command.riskSources.any { !CODE.matches(it) } || command.riskStatusCodes.any { !CODE.matches(it) }) {
            fail("AUTH_SECURITY_EVENT_RISK_CODE_INVALID")
        }
    }

    private fun AuthSecurityEventRecordCommand.toEntity(
        now: LocalDateTime,
        calculatedDueAt: LocalDateTime?,
    ) = AuthSecurityEvent {
        id = UUID.randomUUID().toString()
        tenantId = this@toEntity.tenantId
        userId = this@toEntity.userId
        eventType = this@toEntity.eventType.name
        subjectType = this@toEntity.subjectType
        subjectFingerprint = this@toEntity.subjectFingerprint
        riskLevel = this@toEntity.riskLevel.name
        riskSources = this@toEntity.riskSources.csvOrNull()
        riskStatusCodes = this@toEntity.riskStatusCodes.csvOrNull()
        deduplicationKey = this@toEntity.deduplicationKey
        bucketStart = this@toEntity.bucketStart
        status = AuthSecurityEventStatusEnum.OPEN.name
        workflowVersion = 0
        occurrenceCount = 1
        firstOccurredAt = this@toEntity.occurredAt
        lastOccurredAt = this@toEntity.occurredAt
        acknowledgedBy = null
        acknowledgedAt = null
        acknowledgeReason = null
        resolution = null
        closedBy = null
        closedAt = null
        closeReason = null
        assignedTo = null
        assignedBy = null
        assignedAt = null
        assignmentReason = null
        dueAt = calculatedDueAt
        escalationLevel = 0
        lastEscalatedAt = null
        nextEscalationAt = calculatedDueAt
        createTime = now
        updateTime = now
    }

    private fun AuthSecurityEvent.toSummary() = AuthSecurityEventSummary(
        id = id,
        eventType = parseEventType(eventType),
        userId = userId,
        subjectType = subjectType,
        subjectFingerprint = subjectFingerprint,
        riskLevel = parseRiskLevel(riskLevel),
        riskSources = riskSources.csvValues(),
        riskStatusCodes = riskStatusCodes.csvValues(),
        status = parseStatus(status),
        workflowVersion = workflowVersion,
        occurrenceCount = occurrenceCount,
        bucketStart = bucketStart,
        firstOccurredAt = firstOccurredAt,
        lastOccurredAt = lastOccurredAt,
        acknowledgedBy = acknowledgedBy,
        acknowledgedAt = acknowledgedAt,
        acknowledgeReason = acknowledgeReason,
        resolution = resolution?.let(::parseResolution),
        closedBy = closedBy,
        closedAt = closedAt,
        closeReason = closeReason,
        assignedTo = assignedTo,
        assignedBy = assignedBy,
        assignedAt = assignedAt,
        assignmentReason = assignmentReason,
        dueAt = dueAt,
        escalationLevel = escalationLevel,
        lastEscalatedAt = lastEscalatedAt,
        nextEscalationAt = nextEscalationAt,
    )

    private fun validateWorkflowCommand(
        tenantId: String,
        eventId: String,
        actorUserId: String,
        reason: String,
        expectedVersion: Long,
    ): String {
        requireIdentifier(tenantId, "AUTH_SECURITY_EVENT_TENANT_INVALID")
        requireIdentifier(eventId, "AUTH_SECURITY_EVENT_ID_INVALID")
        requireIdentifier(actorUserId, "AUTH_SECURITY_EVENT_ACTOR_INVALID")
        if (expectedVersion < 0) fail("AUTH_SECURITY_EVENT_VERSION_INVALID")
        return reason.trim().also {
            if (it.isBlank() || it.length > MAX_REASON_LENGTH || it.any(Char::isISOControl)) {
                fail("AUTH_SECURITY_EVENT_REASON_INVALID")
            }
        }
    }

    private fun findForWorkflow(tenantId: String, eventId: String): AuthSecurityEvent =
        dao.findByTenantAndId(tenantId, eventId) ?: fail("AUTH_SECURITY_EVENT_NOT_FOUND")

    private fun requireWorkflowState(
        event: AuthSecurityEvent,
        requiredStatus: AuthSecurityEventStatusEnum,
        expectedVersion: Long,
    ) {
        requireWorkflowVersion(event, expectedVersion)
        if (parseStatus(event.status) != requiredStatus) fail("AUTH_SECURITY_EVENT_STATE_CONFLICT")
    }

    private fun requireWorkflowVersion(event: AuthSecurityEvent, expectedVersion: Long) {
        if (event.workflowVersion != expectedVersion) fail("AUTH_SECURITY_EVENT_VERSION_CONFLICT")
    }

    private fun parseRiskLevel(value: String): WebAuthnAuthenticatorRiskLevelEnum = runCatching {
        WebAuthnAuthenticatorRiskLevelEnum.valueOf(value.trim().uppercase())
    }.getOrElse { fail("AUTH_SECURITY_EVENT_RISK_LEVEL_INVALID", it) }

    private fun parseEventType(value: String): AuthSecurityEventTypeEnum = runCatching {
        AuthSecurityEventTypeEnum.valueOf(value)
    }.getOrElse { fail("AUTH_SECURITY_EVENT_TYPE_INVALID", it) }

    private fun parseStatus(value: String): AuthSecurityEventStatusEnum = runCatching {
        AuthSecurityEventStatusEnum.valueOf(value.trim().uppercase())
    }.getOrElse { fail("AUTH_SECURITY_EVENT_STATUS_INVALID", it) }

    private fun parseResolution(value: String): AuthSecurityEventResolutionEnum = runCatching {
        AuthSecurityEventResolutionEnum.valueOf(value.trim().uppercase())
    }.getOrElse { fail("AUTH_SECURITY_EVENT_RESOLUTION_INVALID", it) }

    private fun requireIdentifier(value: String, errorCode: String) {
        if (value.isBlank() || value.length > 36 || value.any(Char::isISOControl)) fail(errorCode)
    }

    private fun Set<String>.csvOrNull(): String? = takeIf { it.isNotEmpty() }?.toSortedSet()?.joinToString(",")
    private fun String?.csvValues(): Set<String> = this?.split(',')?.filter { it.isNotBlank() }?.toSortedSet() ?: emptySet()
    private fun fail(errorCode: String, cause: Throwable? = null): Nothing =
        throw AuthSecurityEventException(errorCode, cause)

    private companion object {
        val logger = LoggerFactory.getLogger(AuthSecurityEventService::class.java)
        const val MAX_QUERY_LIMIT = 500
        const val DEDUPLICATION_BUCKET_MINUTES = 5
        const val MAX_CODES_PER_DIMENSION = 16
        const val MAX_REASON_LENGTH = 512
        val CODE = Regex("^[A-Za-z0-9_.:-]{1,64}$")
        val FINGERPRINT = Regex("^[A-Za-z0-9_-]{43}$")
    }
}
