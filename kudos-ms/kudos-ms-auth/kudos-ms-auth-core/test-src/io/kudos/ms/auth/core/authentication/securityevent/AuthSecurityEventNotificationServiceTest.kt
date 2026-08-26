package io.kudos.ms.auth.core.authentication.securityevent

import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationProperties
import io.kudos.ms.auth.core.authentication.securityevent.notification.dao.AuthSecurityEventNotificationChannelDao
import io.kudos.ms.auth.core.authentication.securityevent.notification.dao.AuthSecurityEventNotificationDao
import io.kudos.ms.auth.core.authentication.securityevent.notification.model.po.AuthSecurityEventNotification
import io.kudos.ms.auth.core.authentication.securityevent.notification.service.impl.AuthSecurityEventNotificationService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AuthSecurityEventNotificationServiceTest {
    private val instant = Instant.parse("2026-08-25T10:00:00Z")
    private val now = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
    private val clock = Clock.fixed(instant, ZoneOffset.UTC)
    private val dao = mock(AuthSecurityEventNotificationDao::class.java)
    private val properties = AuthSecurityEventNotificationProperties()
    private val channelDao = mock(AuthSecurityEventNotificationChannelDao::class.java)
    private val service = AuthSecurityEventNotificationService(dao, channelDao, properties, clock)

    @Test
    fun claimUsesCasLeaseAndReturnsOnlyWinningRows() {
        val processing = notification().apply {
            status = "PROCESSING"
            attemptCount = 1
            leaseOwner = "worker-1"
            leaseUntil = now.plusMinutes(1)
        }
        `when`(dao.findClaimableIds(now, 10)).thenReturn(listOf("notification-1", "notification-2"))
        `when`(dao.claim("notification-1", "worker-1", now, now.plusMinutes(1))).thenReturn(true)
        `when`(dao.claim("notification-2", "worker-1", now, now.plusMinutes(1))).thenReturn(false)
        `when`(dao.get("notification-1")).thenReturn(processing)

        val result = service.claimPending("worker-1", 10)

        assertEquals(1, result.size)
        assertEquals(1, result.single().attemptCount)
        assertEquals(now.plusMinutes(1), result.single().leaseUntil)
    }

    @Test
    fun failedDeliveryBacksOffAndEventuallyMovesToDeadLetterState() {
        `when`(dao.get("notification-1")).thenReturn(notification().apply {
            status = "PROCESSING"
            attemptCount = 1
            leaseOwner = "worker-1"
        })
        `when`(
            dao.fail("notification-1", "worker-1", "PENDING", now.plusSeconds(30), "SMTP_TIMEOUT", now)
        ).thenReturn(true)

        assertEquals(true, service.fail("notification-1", "worker-1", " SMTP_TIMEOUT "))

        val maxAttempts = properties.maxAttempts
        `when`(dao.get("notification-1")).thenReturn(notification().apply {
            status = "PROCESSING"
            attemptCount = maxAttempts
            leaseOwner = "worker-1"
        })
        `when`(dao.fail("notification-1", "worker-1", "DEAD", null, "PERMANENT_FAILURE", now))
            .thenReturn(true)
        assertEquals(true, service.fail("notification-1", "worker-1", "PERMANENT_FAILURE"))
        verify(dao).fail("notification-1", "worker-1", "DEAD", null, "PERMANENT_FAILURE", now)
    }

    @Test
    fun completionIsOwnedByTheCurrentLeaseWorker() {
        `when`(dao.complete("notification-1", "worker-1", now)).thenReturn(true)
        assertEquals(true, service.complete("notification-1", "worker-1"))
        assertEquals(false, service.complete("notification-1", "worker-2"))
    }

    private fun notification() = AuthSecurityEventNotification {
        id = "notification-1"
        tenantId = "tenant-1"
        eventId = "event-1"
        notificationType = "SLA_ESCALATED"
        escalationLevel = 1
        recipientUserId = "admin-2"
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
