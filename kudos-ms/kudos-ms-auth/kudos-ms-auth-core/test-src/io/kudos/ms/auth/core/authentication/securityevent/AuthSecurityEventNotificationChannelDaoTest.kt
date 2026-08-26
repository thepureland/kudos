package io.kudos.ms.auth.core.authentication.securityevent

import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.spi.WebAuthnAuthenticatorRiskLevelEnum
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventRecordCommand
import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventTypeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationChannelStatusEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationReplayCommand
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationChannelEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.service.iservice.IAuthSecurityEventNotificationAdminService
import io.kudos.ms.auth.core.authentication.securityevent.notification.service.iservice.IAuthSecurityEventNotificationService
import io.kudos.ms.auth.core.authentication.securityevent.service.iservice.IAuthSecurityEventEscalationService
import io.kudos.ms.auth.core.authentication.securityevent.service.iservice.IAuthSecurityEventService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Real Flyway/H2 regression for the V55 per-channel ledger: settle-once semantics, what a retry skips, and the
 * clean slate an administrator's replay starts from.
 */
@EnabledIfDockerInstalled
internal class AuthSecurityEventNotificationChannelDaoTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var eventService: IAuthSecurityEventService

    @Resource
    private lateinit var escalationService: IAuthSecurityEventEscalationService

    @Resource
    private lateinit var notificationService: IAuthSecurityEventNotificationService

    @Resource
    private lateinit var notificationAdminService: IAuthSecurityEventNotificationAdminService

    @Test
    fun v55SettlesEachChannelOnceAndClearsTheLedgerOnReplay() {
        val tenantId = UUID.randomUUID().toString()
        val notificationId = escalatedNotificationId(tenantId)

        assertEquals(emptyList(), notificationService.settledChannels(notificationId))

        assertTrue(settle(notificationId, tenantId, SITE_MESSAGE, delivered = true))
        assertTrue(settle(notificationId, tenantId, EMAIL, delivered = false))
        // The second settle of the same channel is the normal answer after a worker died mid-attempt.
        assertFalse(settle(notificationId, tenantId, SITE_MESSAGE, delivered = false))

        val settled = notificationService.settledChannels(notificationId).associateBy { it.channel }
        assertEquals(2, settled.size)
        assertEquals(AuthSecurityEventNotificationChannelStatusEnum.DELIVERED, settled.getValue(SITE_MESSAGE).status)
        assertNull(settled.getValue(SITE_MESSAGE).lastErrorCode)
        assertEquals(AuthSecurityEventNotificationChannelStatusEnum.DEAD, settled.getValue(EMAIL).status)
        assertEquals("PROVIDER_REJECTED", settled.getValue(EMAIL).lastErrorCode)

        assertTrue(notificationService.dead(notificationId, "integration-worker", "PROVIDER_REJECTED"))
        notificationAdminService.replay(
            AuthSecurityEventNotificationReplayCommand(
                tenantId = tenantId,
                notificationId = notificationId,
                actorUserId = "admin-1",
                reason = "email provider replaced",
            )
        )

        // Replay exists because the previous outcome was judged wrong, so no channel stays refused.
        assertEquals(emptyList(), notificationService.settledChannels(notificationId))
    }

    private fun settle(
        notificationId: String,
        tenantId: String,
        channel: AuthSecurityEventNotificationChannelEnum,
        delivered: Boolean,
    ) = notificationService.settleChannel(
        notificationId = notificationId,
        tenantId = tenantId,
        channel = channel,
        status = if (delivered) {
            AuthSecurityEventNotificationChannelStatusEnum.DELIVERED
        } else {
            AuthSecurityEventNotificationChannelStatusEnum.DEAD
        },
        attemptCount = 1,
        errorCode = if (delivered) null else "PROVIDER_REJECTED",
    )

    private fun escalatedNotificationId(tenantId: String): String {
        eventService.recordOrAggregate(
            AuthSecurityEventRecordCommand(
                tenantId = tenantId,
                userId = UUID.randomUUID().toString(),
                eventType = AuthSecurityEventTypeEnum.WEBAUTHN_AUTHENTICATOR_RISK_BLOCKED,
                subjectType = "WEBAUTHN_CREDENTIAL",
                subjectFingerprint = FINGERPRINT,
                riskLevel = WebAuthnAuthenticatorRiskLevelEnum.CRITICAL,
                riskSources = setOf("FIDO_MDS"),
                riskStatusCodes = setOf("REVOKED"),
                deduplicationKey = DEDUPLICATION_KEY,
                bucketStart = LocalDateTime.of(2026, 8, 24, 10, 0),
                occurredAt = LocalDateTime.of(2026, 8, 24, 10, 1),
            )
        )
        // Escalation and claiming commit outside the test transaction, so rows from other tests in this shared
        // container are visible here. Everything is therefore selected by this tenant's own event.
        val eventId = eventService.listRecent(tenantId, limit = 10).single().id
        escalationService.scanDue(100)
        return notificationService.claimPending("integration-worker", 500).single { it.eventId == eventId }.id
    }

    private companion object {
        val FINGERPRINT = "A".repeat(43)
        val DEDUPLICATION_KEY = "B".repeat(43)
        val SITE_MESSAGE = AuthSecurityEventNotificationChannelEnum.SITE_MESSAGE
        val EMAIL = AuthSecurityEventNotificationChannelEnum.EMAIL
    }
}
