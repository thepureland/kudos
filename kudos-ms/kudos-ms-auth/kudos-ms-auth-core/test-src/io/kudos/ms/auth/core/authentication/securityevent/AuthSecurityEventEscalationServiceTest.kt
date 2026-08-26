package io.kudos.ms.auth.core.authentication.securityevent

import io.kudos.ms.auth.core.authentication.securityevent.dao.AuthSecurityEventDao
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.model.po.AuthSecurityEvent
import io.kudos.ms.auth.core.authentication.securityevent.notification.dao.AuthSecurityEventNotificationDao
import io.kudos.ms.auth.core.authentication.securityevent.notification.model.po.AuthSecurityEventNotification
import io.kudos.ms.auth.core.authentication.securityevent.policy.IAuthSecurityEventEscalationPolicy
import io.kudos.ms.auth.core.authentication.securityevent.service.impl.AuthSecurityEventEscalationProcessor
import io.kudos.ms.auth.core.authentication.securityevent.service.impl.AuthSecurityEventEscalationService
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class AuthSecurityEventEscalationServiceTest {
    private val instant = Instant.parse("2026-08-25T10:00:00Z")
    private val clock = Clock.fixed(instant, ZoneOffset.UTC)
    private val now = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)

    @Test
    fun scanIsBoundedAndCountsOnlyCasWinners() {
        val dao = mock(AuthSecurityEventDao::class.java)
        val processor = mock(AuthSecurityEventEscalationProcessor::class.java)
        val first = event("event-1")
        val second = event("event-2")
        `when`(dao.findEscalationDue(now, 10)).thenReturn(listOf(first, second))
        `when`(processor.escalate(first)).thenReturn(true)
        `when`(processor.escalate(second)).thenReturn(false)
        val service = AuthSecurityEventEscalationService(dao, processor, clock)

        assertEquals(1, service.scanDue(10))
        assertEquals(
            "AUTH_SECURITY_EVENT_SCAN_LIMIT_INVALID",
            assertFailsWith<AuthSecurityEventException> { service.scanDue(501) }.errorCode,
        )
    }

    @Test
    fun processorAdvancesLevelAndWritesNotificationInRequiresNewTransaction() {
        val eventDao = mock(AuthSecurityEventDao::class.java)
        val notificationDao = mock(AuthSecurityEventNotificationDao::class.java)
        val policy = mock(IAuthSecurityEventEscalationPolicy::class.java)
        val event = event("event-1").apply { assignedTo = "admin-2" }
        val next = now.plusHours(4)
        `when`(policy.nextEscalationAt(1, now)).thenReturn(next)
        `when`(eventDao.escalate("event-1", 0, now, next)).thenReturn(true)
        val processor = AuthSecurityEventEscalationProcessor(eventDao, notificationDao, policy, clock)

        assertEquals(true, processor.escalate(event))

        val outbox = ArgumentCaptor.forClass(AuthSecurityEventNotification::class.java)
        verify(notificationDao).insert(outbox.capture() ?: fallbackNotification())
        assertEquals("SLA_ESCALATED", outbox.value.notificationType)
        assertEquals(1, outbox.value.escalationLevel)
        assertEquals("admin-2", outbox.value.recipientUserId)
        assertEquals("PENDING", outbox.value.status)
        val transactional = AuthSecurityEventEscalationProcessor::class.java
            .getMethod("escalate", AuthSecurityEvent::class.java)
            .getAnnotation(Transactional::class.java)
        assertEquals(Propagation.REQUIRES_NEW, transactional.propagation)
    }

    private fun event(eventId: String) = AuthSecurityEvent {
        id = eventId
        tenantId = "tenant-1"
        userId = "user-1"
        eventType = "WEBAUTHN_AUTHENTICATOR_RISK_BLOCKED"
        subjectType = "WEBAUTHN_CREDENTIAL"
        subjectFingerprint = "A".repeat(43)
        riskLevel = "CRITICAL"
        riskSources = "FIDO_MDS"
        riskStatusCodes = "REVOKED"
        deduplicationKey = "B".repeat(43)
        bucketStart = now.minusHours(2)
        status = "OPEN"
        workflowVersion = 0
        occurrenceCount = 1
        firstOccurredAt = now.minusHours(2)
        lastOccurredAt = now.minusHours(2)
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
        dueAt = now.minusHours(1)
        escalationLevel = 0
        lastEscalatedAt = null
        nextEscalationAt = dueAt
        createTime = now.minusHours(2)
        updateTime = now.minusHours(2)
    }

    private fun fallbackNotification() = AuthSecurityEventNotification {
        id = "notification-1"
        tenantId = "tenant-1"
        eventId = "event-1"
        notificationType = "SLA_ESCALATED"
        escalationLevel = 1
        recipientUserId = null
        dueAt = now.minusHours(1)
        escalatedAt = now
        status = "PENDING"
        attemptCount = 0
        nextAttemptAt = now
        leaseOwner = null
        leaseUntil = null
        deliveredAt = null
        lastErrorCode = null
        replayCount = 0
        createTime = now
        updateTime = now
    }
}
