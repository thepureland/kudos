package io.kudos.ms.auth.core.authentication.securityevent

import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskLevelEnum
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventAcknowledgeCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventAssignCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventCloseCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventRecordCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventResolutionEnum
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventStatusEnum
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventTypeEnum
import io.kudos.ms.auth.core.authentication.securityevent.service.iservice.IAuthSecurityEventService
import io.kudos.ms.auth.core.authentication.securityevent.service.iservice.IAuthSecurityEventEscalationService
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationReplayCommand
import io.kudos.ms.auth.core.authentication.securityevent.notification.service.iservice.IAuthSecurityEventNotificationAdminService
import io.kudos.ms.auth.core.authentication.securityevent.notification.service.iservice.IAuthSecurityEventNotificationService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@EnabledIfDockerInstalled
internal class AuthSecurityEventDaoTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var service: IAuthSecurityEventService

    @Resource
    private lateinit var escalationService: IAuthSecurityEventEscalationService

    @Resource
    private lateinit var notificationService: IAuthSecurityEventNotificationService

    @Resource
    private lateinit var notificationAdminService: IAuthSecurityEventNotificationAdminService

    @Test
    fun v51PersistsEscalationOutboxReplaysDeadNotificationAndTransitionsTenantScopedEvents() {
        val tenantId = UUID.randomUUID().toString()
        val first = command(tenantId, LocalDateTime.of(2026, 8, 24, 10, 1))
        val second = first.copy(occurredAt = LocalDateTime.of(2026, 8, 24, 10, 4))

        service.recordOrAggregate(first)
        service.recordOrAggregate(second)

        val stored = service.listRecent(tenantId, riskLevel = "CRITICAL", limit = 10).single()
        assertEquals(2, stored.occurrenceCount)
        assertEquals(first.occurredAt, stored.firstOccurredAt)
        assertEquals(second.occurredAt, stored.lastOccurredAt)
        assertEquals(FINGERPRINT, stored.subjectFingerprint)
        assertEquals(setOf("FIDO_MDS"), stored.riskSources)
        assertEquals(first.occurredAt.plusHours(1), stored.dueAt)
        assertEquals(emptyList(), service.listRecent(UUID.randomUUID().toString(), limit = 10))

        // Escalation, claiming and settling commit outside the test transaction, so other tests sharing this
        // container leave rows behind. Everything below is therefore scoped to this tenant's own event rather
        // than to global counts.
        assertTrue(escalationService.scanDue(100) >= 1)
        val escalated = service.listRecent(tenantId, limit = 10).single()
        assertEquals(1, escalated.escalationLevel)
        assertEquals(stored.workflowVersion, escalated.workflowVersion)
        assertEquals(escalated.lastEscalatedAt?.plusHours(4), escalated.nextEscalationAt)
        escalationService.scanDue(100)
        assertEquals(1, service.listRecent(tenantId, limit = 10).single().escalationLevel)

        val delivery = notificationService.claimPending("integration-worker", 500)
            .single { it.eventId == stored.id }
        assertEquals(tenantId, delivery.tenantId)
        assertEquals(stored.id, delivery.eventId)
        assertEquals(null, delivery.recipientUserId)
        assertEquals(1, delivery.escalationLevel)
        assertEquals(true, notificationService.dead(delivery.id, "integration-worker", "INVALID_DESTINATION"))
        val dead = notificationAdminService.listDead(tenantId, stored.id, 10).single()
        assertEquals("INVALID_DESTINATION", dead.lastErrorCode)
        assertEquals(emptyList(), notificationAdminService.listDead(UUID.randomUUID().toString(), stored.id, 10))
        val replayed = notificationAdminService.replay(
            AuthSecurityEventNotificationReplayCommand(
                tenantId = tenantId,
                notificationId = delivery.id,
                actorUserId = "admin-1",
                reason = "notification route repaired",
            )
        )
        assertEquals(0, replayed.attemptCount)
        assertEquals(1, replayed.replayCount)
        assertEquals(emptyList(), notificationAdminService.listDead(tenantId, stored.id, 10))
        val replayDelivery = notificationService.claimPending("integration-worker", 500)
            .single { it.eventId == stored.id }
        assertEquals(delivery.id, replayDelivery.id)
        assertEquals(true, notificationService.complete(replayDelivery.id, "integration-worker"))
        assertEquals(
            emptyList(),
            notificationService.claimPending("integration-worker", 500).filter { it.eventId == stored.id },
        )

        val assigned = service.assign(
            AuthSecurityEventAssignCommand(
                tenantId = tenantId,
                eventId = stored.id,
                actorUserId = "admin-1",
                assigneeUserId = "admin-2",
                reason = "primary responder",
                expectedVersion = 0,
            )
        )
        assertEquals("admin-2", assigned.assignedTo)
        assertEquals(1, assigned.workflowVersion)
        assertEquals(
            stored.id,
            service.listRecent(tenantId, assigneeUserId = "admin-2", overdueOnly = true, limit = 10).single().id,
        )

        val acknowledged = service.acknowledge(
            AuthSecurityEventAcknowledgeCommand(
                tenantId = tenantId,
                eventId = stored.id,
                actorUserId = "admin-1",
                reason = "investigating model status",
                expectedVersion = assigned.workflowVersion,
            )
        )
        assertEquals(AuthSecurityEventStatusEnum.ACKNOWLEDGED, acknowledged.status)
        assertEquals(2, acknowledged.workflowVersion)
        assertEquals("admin-1", acknowledged.acknowledgedBy)

        val closed = service.close(
            AuthSecurityEventCloseCommand(
                tenantId = tenantId,
                eventId = stored.id,
                actorUserId = "admin-2",
                resolution = "MITIGATED",
                reason = "credential replaced through a separate approved workflow",
                expectedVersion = acknowledged.workflowVersion,
            )
        )
        assertEquals(AuthSecurityEventStatusEnum.CLOSED, closed.status)
        assertEquals(AuthSecurityEventResolutionEnum.MITIGATED, closed.resolution)
        assertEquals(3, closed.workflowVersion)
        assertEquals("admin-2", closed.closedBy)
        assertEquals(stored.id, service.listRecent(tenantId, status = "closed", limit = 10).single().id)
        assertEquals(emptyList(), service.listRecent(tenantId, status = "open", limit = 10))
        assertEquals(emptyList(), service.listRecent(tenantId, overdueOnly = true, limit = 10))
    }

    private fun command(tenantId: String, occurredAt: LocalDateTime) = AuthSecurityEventRecordCommand(
        tenantId = tenantId,
        userId = "user-1",
        eventType = AuthSecurityEventTypeEnum.WEBAUTHN_AUTHENTICATOR_RISK_BLOCKED,
        subjectType = "WEBAUTHN_CREDENTIAL",
        subjectFingerprint = FINGERPRINT,
        riskLevel = WebAuthnAuthenticatorRiskLevelEnum.CRITICAL,
        riskSources = setOf("FIDO_MDS"),
        riskStatusCodes = setOf("REVOKED"),
        deduplicationKey = DEDUPLICATION_KEY,
        bucketStart = LocalDateTime.of(2026, 8, 24, 10, 0),
        occurredAt = occurredAt,
    )

    private companion object {
        val FINGERPRINT = "A".repeat(43)
        val DEDUPLICATION_KEY = "B".repeat(43)
    }
}
