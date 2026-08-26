package io.kudos.ms.auth.core.authentication.securityevent

import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationDelivery
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationTypeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationChannelEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationDestinationEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.DefaultAuthSecurityEventNotificationRoutePolicy
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

internal class AuthSecurityEventNotificationRoutePolicyTest {
    private val policy = DefaultAuthSecurityEventNotificationRoutePolicy()

    @Test
    fun assignedAndUnassignedNotificationsUseExplicitSafeRoutes() {
        val assigned = policy.resolve(delivery("admin-2"))
        val unassigned = policy.resolve(delivery(null))

        assertEquals(AuthSecurityEventNotificationDestinationEnum.USER, assigned.destination)
        assertEquals(setOf(AuthSecurityEventNotificationChannelEnum.SITE_MESSAGE), assigned.channels)
        assertEquals(setOf("admin-2"), assigned.recipientUserIds)
        assertEquals(AuthSecurityEventNotificationDestinationEnum.TENANT_SECURITY_QUEUE, unassigned.destination)
        assertEquals(setOf(AuthSecurityEventNotificationChannelEnum.EVENT_BUS), unassigned.channels)
        assertEquals(emptySet(), unassigned.recipientUserIds)
    }

    private fun delivery(recipientUserId: String?) = AuthSecurityEventNotificationDelivery(
        id = "notification-1",
        tenantId = "tenant-1",
        eventId = "event-1",
        notificationType = AuthSecurityEventNotificationTypeEnum.SLA_ESCALATED,
        escalationLevel = 1,
        recipientUserId = recipientUserId,
        dueAt = LocalDateTime.parse("2026-08-25T09:00:00"),
        escalatedAt = LocalDateTime.parse("2026-08-25T10:00:00"),
        attemptCount = 1,
        leaseUntil = LocalDateTime.parse("2026-08-25T10:01:00"),
    )
}
