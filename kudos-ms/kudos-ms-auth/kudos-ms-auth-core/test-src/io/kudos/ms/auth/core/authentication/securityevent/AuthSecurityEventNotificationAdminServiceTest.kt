package io.kudos.ms.auth.core.authentication.securityevent

import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationReplayCommand
import io.kudos.ms.auth.core.authentication.securityevent.notification.dao.AuthSecurityEventNotificationChannelDao
import io.kudos.ms.auth.core.authentication.securityevent.notification.dao.AuthSecurityEventNotificationDao
import io.kudos.ms.auth.core.authentication.securityevent.notification.model.po.AuthSecurityEventNotification
import io.kudos.ms.auth.core.authentication.securityevent.notification.service.impl.AuthSecurityEventNotificationAdminService
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class AuthSecurityEventNotificationAdminServiceTest {
    private val instant = Instant.parse("2026-08-25T10:00:00Z")
    private val now = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
    private val dao = mock(AuthSecurityEventNotificationDao::class.java)
    private val channelDao = mock(AuthSecurityEventNotificationChannelDao::class.java)
    private val service =
        AuthSecurityEventNotificationAdminService(dao, channelDao, Clock.fixed(instant, ZoneOffset.UTC))

    @Test
    fun deadQueryIsTenantBoundedAndMapsOnlyPublicOperationalFields() {
        `when`(dao.findDead("tenant-1", "event-1", 50)).thenReturn(listOf(notification()))

        val result = service.listDead("tenant-1", "event-1", 50).single()

        assertEquals("notification-1", result.id)
        assertEquals("DEAD", result.status.name)
        assertEquals("CHANNEL_TIMEOUT", result.lastErrorCode)
        assertEquals(8, result.attemptCount)
        verify(dao).findDead("tenant-1", "event-1", 50)
    }

    @Test
    fun replayUsesDeadOnlyCasAndAppendsActorReasonAuditInTheSameTransaction() {
        val dead = notification()
        val pending = notification().apply {
            status = "PENDING"
            attemptCount = 0
            nextAttemptAt = now
            lastErrorCode = null
            replayCount = 1
            updateTime = now
        }
        `when`(dao.findByTenantAndId("tenant-1", "notification-1")).thenReturn(dead, pending)
        `when`(dao.replay("tenant-1", "notification-1", now)).thenReturn(true)

        val result = service.replay(
            AuthSecurityEventNotificationReplayCommand(
                "tenant-1",
                "notification-1",
                "admin-1",
                " channel recovered ",
            )
        )

        assertEquals("PENDING", result.status.name)
        assertEquals(0, result.attemptCount)
        assertEquals(1, result.replayCount)
        val auditId = ArgumentCaptor.forClass(String::class.java)
        verify(dao).insertReplayAudit(
            auditId.capture() ?: "",
            eq("tenant-1") ?: "",
            eq("notification-1") ?: "",
            eq("admin-1") ?: "",
            eq("channel recovered") ?: "",
            eq(now) ?: now,
        )
        assertEquals(36, auditId.value.length)
    }

    @Test
    fun missingCrossTenantAndNonDeadRowsCannotBeReplayed() {
        val command = AuthSecurityEventNotificationReplayCommand(
            "tenant-1",
            "notification-1",
            "admin-1",
            "channel recovered",
        )
        `when`(dao.findByTenantAndId("tenant-1", "notification-1"))
            .thenReturn(null, notification().apply { status = "PENDING" })

        assertEquals(
            "AUTH_SECURITY_EVENT_NOTIFICATION_NOT_FOUND",
            assertFailsWith<AuthSecurityEventException> { service.replay(command) }.errorCode,
        )
        assertEquals(
            "AUTH_SECURITY_EVENT_NOTIFICATION_STATE_CONFLICT",
            assertFailsWith<AuthSecurityEventException> { service.replay(command) }.errorCode,
        )
        verify(dao, never()).replay("tenant-1", "notification-1", now)
    }

    private fun notification() = AuthSecurityEventNotification {
        id = "notification-1"
        tenantId = "tenant-1"
        eventId = "event-1"
        notificationType = "SLA_ESCALATED"
        escalationLevel = 1
        recipientUserId = "admin-2"
        dueAt = now.minusHours(1)
        escalatedAt = now.minusMinutes(10)
        status = "DEAD"
        attemptCount = 8
        nextAttemptAt = null
        leaseOwner = null
        leaseUntil = null
        deliveredAt = null
        lastErrorCode = "CHANNEL_TIMEOUT"
        replayCount = 0
        createTime = now.minusMinutes(10)
        updateTime = now.minusMinutes(1)
    }
}
