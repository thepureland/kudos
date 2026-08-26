package io.kudos.ms.auth.core.authentication.securityevent

import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskLevelEnum
import io.kudos.ms.auth.core.authentication.securityevent.dao.AuthSecurityEventDao
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventAcknowledgeCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventAssignCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventCloseCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventRecordCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventStatusEnum
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventTypeEnum
import io.kudos.ms.auth.core.authentication.securityevent.model.po.AuthSecurityEvent
import io.kudos.ms.auth.core.authentication.securityevent.event.AuthSecurityEventAssigned
import io.kudos.ms.auth.core.authentication.securityevent.policy.AuthSecurityEventSlaProperties
import io.kudos.ms.auth.core.authentication.securityevent.policy.DefaultAuthSecurityEventSlaPolicy
import io.kudos.ms.auth.core.authentication.securityevent.service.impl.AuthSecurityEventService
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.context.ApplicationEventPublisher
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Clock
import java.time.Instant
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class AuthSecurityEventServiceTest {
    private val now = Instant.parse("2026-08-25T10:03:00Z")
    private val dao = mock(AuthSecurityEventDao::class.java)
    private val publisher = mock(ApplicationEventPublisher::class.java)
    private val slaPolicy = DefaultAuthSecurityEventSlaPolicy(AuthSecurityEventSlaProperties())
    private val service = AuthSecurityEventService(dao, slaPolicy, publisher, Clock.fixed(now, ZoneOffset.UTC))

    @Test
    fun insertsFirstOccurrenceAndAggregatesExistingBucket() {
        val command = command()
        `when`(dao.incrementOccurrence(command, localNow())).thenReturn(false, true)

        service.recordOrAggregate(command)
        service.recordOrAggregate(command)

        val captor = ArgumentCaptor.forClass(AuthSecurityEvent::class.java)
        verify(dao).insert(captor.capture() ?: fallbackEntity())
        assertEquals("tenant-1", captor.value.tenantId)
        assertEquals("WEBAUTHN_AUTHENTICATOR_RISK_BLOCKED", captor.value.eventType)
        assertEquals("CRITICAL", captor.value.riskLevel)
        assertEquals(1, captor.value.occurrenceCount)
        assertEquals(command.occurredAt, captor.value.firstOccurredAt)
        assertEquals(command.occurredAt.plusHours(1), captor.value.dueAt)
    }

    @Test
    fun defaultSlaPolicyIsConfigurableAndCanBeDisabled() {
        val properties = AuthSecurityEventSlaProperties().apply {
            warning = Duration.ofMinutes(30)
        }
        val policy = DefaultAuthSecurityEventSlaPolicy(properties)
        val warning = command().copy(riskLevel = WebAuthnAuthenticatorRiskLevelEnum.WARNING)

        assertEquals(warning.occurredAt.plusMinutes(30), policy.calculateDueAt(warning))
        properties.enabled = false
        assertEquals(null, policy.calculateDueAt(warning))
    }

    @Test
    fun concurrentInsertRetryRequiresTheWinningRowToExist() {
        val command = command()
        `when`(dao.incrementOccurrence(command, localNow())).thenReturn(false, true)

        val missing = assertFailsWith<AuthSecurityEventException> {
            service.aggregateAfterConcurrentInsert(command)
        }
        service.aggregateAfterConcurrentInsert(command)

        assertEquals("AUTH_SECURITY_EVENT_CONCURRENT_RECORD_NOT_FOUND", missing.errorCode)
    }

    @Test
    fun recentQueryIsBoundedAndMapsStoredValues() {
        `when`(
            dao.findRecent("tenant-1", "user-1", "CRITICAL", "OPEN", "admin-1", localNow(), 50)
        ).thenReturn(listOf(stored()))

        val result = service.listRecent(
            "tenant-1",
            "user-1",
            " critical ",
            " open ",
            "admin-1",
            true,
            50,
        )

        assertEquals(1, result.size)
        assertEquals(setOf("FIDO_MDS"), result.single().riskSources)
        assertEquals(setOf("REVOKED"), result.single().riskStatusCodes)
        assertEquals(3, result.single().occurrenceCount)
        verify(dao).findRecent("tenant-1", "user-1", "CRITICAL", "OPEN", "admin-1", localNow(), 50)

        val invalid = assertFailsWith<AuthSecurityEventException> {
            service.listRecent("tenant-1", limit = 501)
        }
        assertEquals("AUTH_SECURITY_EVENT_LIMIT_INVALID", invalid.errorCode)
    }

    @Test
    fun rejectsMalformedDeduplicationWindowsAndUnboundedRiskCodes() {
        val invalidBucket = assertFailsWith<AuthSecurityEventException> {
            service.recordOrAggregate(command().copy(bucketStart = LocalDateTime.of(2026, 8, 25, 10, 1)))
        }
        val outsideBucket = assertFailsWith<AuthSecurityEventException> {
            service.recordOrAggregate(command().copy(occurredAt = LocalDateTime.of(2026, 8, 25, 10, 5)))
        }
        val tooManyCodes = assertFailsWith<AuthSecurityEventException> {
            service.recordOrAggregate(command().copy(riskSources = (1..17).map { "SOURCE_$it" }.toSet()))
        }

        assertEquals("AUTH_SECURITY_EVENT_BUCKET_INVALID", invalidBucket.errorCode)
        assertEquals("AUTH_SECURITY_EVENT_OCCURRED_AT_INVALID", outsideBucket.errorCode)
        assertEquals("AUTH_SECURITY_EVENT_RISK_CODE_LIMIT_EXCEEDED", tooManyCodes.errorCode)
        verify(dao, never()).insert(any(AuthSecurityEvent::class.java) ?: fallbackEntity())
    }

    @Test
    fun acknowledgesSettledOpenEventWithOptimisticVersion() {
        val open = workflowEntity(AuthSecurityEventStatusEnum.OPEN, 0)
        val acknowledged = workflowEntity(AuthSecurityEventStatusEnum.ACKNOWLEDGED, 1)
        `when`(dao.findByTenantAndId("tenant-1", "event-1")).thenReturn(open, acknowledged)
        `when`(
            dao.acknowledge(
                "tenant-1",
                "event-1",
                0,
                "admin-1",
                "investigating",
                localNow(),
            )
        ).thenReturn(true)

        val result = service.acknowledge(
            AuthSecurityEventAcknowledgeCommand(
                tenantId = "tenant-1",
                eventId = "event-1",
                actorUserId = "admin-1",
                reason = " investigating ",
                expectedVersion = 0,
            )
        )

        assertEquals(AuthSecurityEventStatusEnum.ACKNOWLEDGED, result.status)
        assertEquals(1, result.workflowVersion)
        assertEquals("admin-1", result.acknowledgedBy)
    }

    @Test
    fun closesOnlyAcknowledgedEventAndNormalizesResolution() {
        val acknowledged = workflowEntity(AuthSecurityEventStatusEnum.ACKNOWLEDGED, 1)
        val closed = workflowEntity(AuthSecurityEventStatusEnum.CLOSED, 2)
        `when`(dao.findByTenantAndId("tenant-1", "event-1")).thenReturn(acknowledged, closed)
        `when`(
            dao.close(
                "tenant-1",
                "event-1",
                1,
                "admin-2",
                "MITIGATED",
                "credential replaced",
                localNow(),
            )
        ).thenReturn(true)

        val result = service.close(
            AuthSecurityEventCloseCommand(
                tenantId = "tenant-1",
                eventId = "event-1",
                actorUserId = "admin-2",
                resolution = " mitigated ",
                reason = "credential replaced",
                expectedVersion = 1,
            )
        )

        assertEquals(AuthSecurityEventStatusEnum.CLOSED, result.status)
        assertEquals(2, result.workflowVersion)
        assertEquals("MITIGATED", result.resolution?.name)
    }

    @Test
    fun assignsActiveEventWithOptimisticVersionAndPublishesExtensionEvent() {
        val open = workflowEntity(AuthSecurityEventStatusEnum.OPEN, 0)
        val assigned = workflowEntity(AuthSecurityEventStatusEnum.OPEN, 1).apply {
            assignedTo = "admin-2"
            assignedBy = "admin-1"
            assignedAt = localNow()
            assignmentReason = "primary responder"
        }
        `when`(dao.findByTenantAndId("tenant-1", "event-1")).thenReturn(open, assigned)
        `when`(
            dao.assign("tenant-1", "event-1", 0, "admin-1", "admin-2", "primary responder", localNow())
        ).thenReturn(true)

        TransactionSynchronizationManager.initSynchronization()
        val result = try {
            val current = service.assign(
                AuthSecurityEventAssignCommand(
                    tenantId = "tenant-1",
                    eventId = "event-1",
                    actorUserId = "admin-1",
                    assigneeUserId = "admin-2",
                    reason = " primary responder ",
                    expectedVersion = 0,
                )
            )
            verify(publisher, never()).publishEvent(any(Any::class.java) ?: Any())
            TransactionSynchronizationManager.getSynchronizations().forEach { it.afterCommit() }
            current
        } finally {
            TransactionSynchronizationManager.clearSynchronization()
        }

        assertEquals("admin-2", result.assignedTo)
        assertEquals(1, result.workflowVersion)
        val event = ArgumentCaptor.forClass(AuthSecurityEventAssigned::class.java)
        verify(publisher).publishEvent(event.capture() ?: fallbackAssignedEvent())
        assertEquals("admin-2", event.value.assigneeUserId)
        assertEquals(null, event.value.previousAssigneeUserId)
    }

    @Test
    fun rejectsActiveBucketAndStaleWorkflowVersion() {
        `when`(dao.findByTenantAndId("tenant-1", "event-1")).thenReturn(stored())
        val activeBucket = assertFailsWith<AuthSecurityEventException> {
            service.acknowledge(acknowledgeCommand())
        }

        `when`(dao.findByTenantAndId("tenant-1", "event-1"))
            .thenReturn(workflowEntity(AuthSecurityEventStatusEnum.ACKNOWLEDGED, 1))
        val stale = assertFailsWith<AuthSecurityEventException> {
            service.acknowledge(acknowledgeCommand())
        }

        assertEquals("AUTH_SECURITY_EVENT_BUCKET_NOT_SETTLED", activeBucket.errorCode)
        assertEquals("AUTH_SECURITY_EVENT_VERSION_CONFLICT", stale.errorCode)
    }

    @Test
    fun rejectsAssignmentOfClosedOrStaleEvent() {
        `when`(dao.findByTenantAndId("tenant-1", "event-1"))
            .thenReturn(workflowEntity(AuthSecurityEventStatusEnum.CLOSED, 2))
        val closed = assertFailsWith<AuthSecurityEventException> {
            service.assign(assignCommand(expectedVersion = 2))
        }

        `when`(dao.findByTenantAndId("tenant-1", "event-1"))
            .thenReturn(workflowEntity(AuthSecurityEventStatusEnum.OPEN, 3))
        val stale = assertFailsWith<AuthSecurityEventException> {
            service.assign(assignCommand(expectedVersion = 2))
        }

        assertEquals("AUTH_SECURITY_EVENT_STATE_CONFLICT", closed.errorCode)
        assertEquals("AUTH_SECURITY_EVENT_VERSION_CONFLICT", stale.errorCode)
        verify(dao, never()).assign(
            any(String::class.java) ?: "",
            any(String::class.java) ?: "",
            any(Long::class.java) ?: 0L,
            any(String::class.java) ?: "",
            any(String::class.java) ?: "",
            any(String::class.java) ?: "",
            any(LocalDateTime::class.java) ?: localNow(),
        )
    }

    @Test
    fun persistenceMethodsUseIndependentTransactions() {
        val record = AuthSecurityEventService::class.java
            .getMethod("recordOrAggregate", AuthSecurityEventRecordCommand::class.java)
            .getAnnotation(Transactional::class.java)
        val retry = AuthSecurityEventService::class.java
            .getMethod("aggregateAfterConcurrentInsert", AuthSecurityEventRecordCommand::class.java)
            .getAnnotation(Transactional::class.java)

        assertEquals(Propagation.REQUIRES_NEW, record.propagation)
        assertEquals(Propagation.REQUIRES_NEW, retry.propagation)
        verify(dao, never()).insert(any(AuthSecurityEvent::class.java) ?: fallbackEntity())
    }

    private fun command() = AuthSecurityEventRecordCommand(
        tenantId = "tenant-1",
        userId = "user-1",
        eventType = AuthSecurityEventTypeEnum.WEBAUTHN_AUTHENTICATOR_RISK_BLOCKED,
        subjectType = "WEBAUTHN_CREDENTIAL",
        subjectFingerprint = FINGERPRINT,
        riskLevel = WebAuthnAuthenticatorRiskLevelEnum.CRITICAL,
        riskSources = setOf("FIDO_MDS"),
        riskStatusCodes = setOf("REVOKED"),
        deduplicationKey = DEDUPLICATION_KEY,
        bucketStart = LocalDateTime.of(2026, 8, 25, 10, 0),
        occurredAt = LocalDateTime.of(2026, 8, 25, 10, 2),
    )

    private fun stored() = AuthSecurityEvent {
        id = "event-1"
        tenantId = "tenant-1"
        userId = "user-1"
        eventType = "WEBAUTHN_AUTHENTICATOR_RISK_BLOCKED"
        subjectType = "WEBAUTHN_CREDENTIAL"
        subjectFingerprint = FINGERPRINT
        riskLevel = "CRITICAL"
        riskSources = "FIDO_MDS"
        riskStatusCodes = "REVOKED"
        deduplicationKey = DEDUPLICATION_KEY
        bucketStart = LocalDateTime.of(2026, 8, 25, 10, 0)
        status = "OPEN"
        workflowVersion = 0
        occurrenceCount = 3
        firstOccurredAt = LocalDateTime.of(2026, 8, 25, 10, 1)
        lastOccurredAt = LocalDateTime.of(2026, 8, 25, 10, 3)
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
        dueAt = LocalDateTime.of(2026, 8, 25, 11, 2)
        escalationLevel = 0
        lastEscalatedAt = null
        nextEscalationAt = dueAt
        createTime = localNow()
        updateTime = localNow()
    }

    private fun workflowEntity(status: AuthSecurityEventStatusEnum, version: Long) = stored().apply {
        bucketStart = LocalDateTime.of(2026, 8, 25, 9, 55)
        this.status = status.name
        workflowVersion = version
        if (status != AuthSecurityEventStatusEnum.OPEN) {
            acknowledgedBy = "admin-1"
            acknowledgedAt = LocalDateTime.of(2026, 8, 25, 10, 1)
            acknowledgeReason = "investigating"
        }
        if (status == AuthSecurityEventStatusEnum.CLOSED) {
            resolution = "MITIGATED"
            closedBy = "admin-2"
            closedAt = localNow()
            closeReason = "credential replaced"
        }
    }

    private fun acknowledgeCommand() = AuthSecurityEventAcknowledgeCommand(
        tenantId = "tenant-1",
        eventId = "event-1",
        actorUserId = "admin-1",
        reason = "investigating",
        expectedVersion = 0,
    )

    private fun assignCommand(expectedVersion: Long) = AuthSecurityEventAssignCommand(
        tenantId = "tenant-1",
        eventId = "event-1",
        actorUserId = "admin-1",
        assigneeUserId = "admin-2",
        reason = "primary responder",
        expectedVersion = expectedVersion,
    )

    private fun fallbackEntity() = stored()
    private fun fallbackAssignedEvent() = AuthSecurityEventAssigned(
        tenantId = "tenant-1",
        eventId = "event-1",
        actorUserId = "admin-1",
        assigneeUserId = "admin-2",
        previousAssigneeUserId = null,
        dueAt = null,
        occurredAt = localNow(),
    )
    private fun localNow() = LocalDateTime.ofInstant(now, ZoneOffset.UTC)

    private companion object {
        val FINGERPRINT = "A".repeat(43)
        val DEDUPLICATION_KEY = "B".repeat(43)
    }
}
