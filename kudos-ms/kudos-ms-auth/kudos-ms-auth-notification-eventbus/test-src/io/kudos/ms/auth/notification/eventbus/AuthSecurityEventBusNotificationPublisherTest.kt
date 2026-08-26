package io.kudos.ms.auth.notification.eventbus

import io.kudos.ability.distributed.stream.common.support.StreamProducerHelper
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationDelivery
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationTypeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationChannelEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationDestinationEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationPublication
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRoute
import io.kudos.ms.auth.core.authentication.securityevent.notification.spi.AuthSecurityEventNotificationPublishException
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class AuthSecurityEventBusNotificationPublisherTest {
    private val producerHelper = mock(StreamProducerHelper::class.java)
    private val properties = AuthSecurityEventBusNotificationProperties()
    private val publisher = AuthSecurityEventBusNotificationPublisher(producerHelper, properties)

    @Test
    fun onlyTheBusChannelIsClaimedAndEveryDestinationMayUseIt() {
        val user = AuthSecurityEventNotificationDestinationEnum.USER
        val queue = AuthSecurityEventNotificationDestinationEnum.TENANT_SECURITY_QUEUE

        assertTrue(publisher.supports(queue, AuthSecurityEventNotificationChannelEnum.EVENT_BUS))
        assertTrue(publisher.supports(user, AuthSecurityEventNotificationChannelEnum.EVENT_BUS))
        // Claiming these would mean quietly swallowing notifications this adapter cannot deliver.
        assertFalse(publisher.supports(queue, AuthSecurityEventNotificationChannelEnum.WORK_ORDER))
        assertFalse(publisher.supports(user, AuthSecurityEventNotificationChannelEnum.SITE_MESSAGE))
        assertFalse(publisher.supports(user, AuthSecurityEventNotificationChannelEnum.EMAIL))
    }

    @Test
    fun theBusMessageCarriesIdentifiersAndTheRoutingDecisionOnly() {
        `when`(producerHelper.sendMessage(anyString(), any<AuthSecurityEventBusNotificationMessage>()))
            .thenReturn(true)

        publisher.publish(publication())

        val sent = capturedMessage()
        assertEquals("notification-1:EVENT_BUS", sent.idempotencyKey)
        assertEquals("notification-1", sent.notificationId)
        assertEquals("tenant-1", sent.tenantId)
        assertEquals("event-1", sent.eventId)
        assertEquals("SLA_ESCALATED", sent.notificationType)
        assertEquals(2, sent.escalationLevel)
        assertEquals("TENANT_SECURITY_QUEUE", sent.destination)
        assertEquals("EVENT_BUS", sent.channel)
        assertEquals("DUTY", sent.routeCode)
        assertEquals(setOf("duty-1"), sent.recipientUserIds)
    }

    @Test
    fun aRefusedOrUnavailableBusIsRetryableWhileMisconfigurationIsPermanent() {
        `when`(producerHelper.sendMessage(anyString(), any<AuthSecurityEventBusNotificationMessage>()))
            .thenReturn(false)
        val refused = assertFailsWithPublishException { publisher.publish(publication()) }

        `when`(producerHelper.sendMessage(anyString(), any<AuthSecurityEventBusNotificationMessage>()))
            .thenThrow(IllegalStateException("broker down"))
        val unavailable = assertFailsWithPublishException { publisher.publish(publication()) }

        val misconfigured = AuthSecurityEventBusNotificationPublisher(
            producerHelper,
            AuthSecurityEventBusNotificationProperties().apply { bindingName = "not a binding name" },
        )
        val invalidConfig = assertFailsWithPublishException { misconfigured.publish(publication()) }

        // Backpressure and a missing binding are indistinguishable at this boundary, so both stay retryable
        // and a permanently missing binding surfaces as a dead notification an operator can see.
        assertTrue(refused.retryable)
        assertEquals("AUTH_SECURITY_EVENT_NOTIFICATION_BUS_NOT_ACCEPTED", refused.errorCode)
        assertTrue(unavailable.retryable)
        assertEquals("AUTH_SECURITY_EVENT_NOTIFICATION_BUS_UNAVAILABLE", unavailable.errorCode)
        assertFalse(invalidConfig.retryable)
        assertEquals("AUTH_SECURITY_EVENT_NOTIFICATION_BUS_CONFIG_INVALID", invalidConfig.errorCode)
    }

    @Test
    fun aPublicationForAnotherChannelIsRefusedPermanentlyRatherThanSentAnyway() {
        val failure = assertFailsWithPublishException {
            publisher.publish(publication(AuthSecurityEventNotificationChannelEnum.SITE_MESSAGE))
        }

        assertFalse(failure.retryable)
        assertEquals("AUTH_SECURITY_EVENT_NOTIFICATION_BUS_CHANNEL_UNSUPPORTED", failure.errorCode)
    }

    private fun capturedMessage(): AuthSecurityEventBusNotificationMessage {
        val invocation = org.mockito.Mockito.mockingDetails(producerHelper).invocations
            .last { it.method.name == "sendMessage" }
        assertEquals(properties.bindingName, invocation.arguments[0])
        return invocation.arguments[1] as AuthSecurityEventBusNotificationMessage
    }

    private fun assertFailsWithPublishException(
        block: () -> Unit,
    ): AuthSecurityEventNotificationPublishException = try {
        block()
        throw AssertionError("expected a publish exception")
    } catch (e: AuthSecurityEventNotificationPublishException) {
        e
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> any(): T = org.mockito.Mockito.any<T>() ?: (null as T)

    private fun publication(
        channel: AuthSecurityEventNotificationChannelEnum = AuthSecurityEventNotificationChannelEnum.EVENT_BUS,
    ) = AuthSecurityEventNotificationPublication(
        notification = AuthSecurityEventNotificationDelivery(
            id = "notification-1",
            tenantId = "tenant-1",
            eventId = "event-1",
            notificationType = AuthSecurityEventNotificationTypeEnum.SLA_ESCALATED,
            escalationLevel = 2,
            recipientUserId = null,
            dueAt = LocalDateTime.parse("2026-08-25T09:00:00"),
            escalatedAt = LocalDateTime.parse("2026-08-25T10:00:00"),
            attemptCount = 1,
            leaseUntil = LocalDateTime.parse("2026-08-25T10:01:00"),
        ),
        route = AuthSecurityEventNotificationRoute(
            routeCode = "DUTY",
            destination = AuthSecurityEventNotificationDestinationEnum.TENANT_SECURITY_QUEUE,
            channels = setOf(AuthSecurityEventNotificationChannelEnum.EVENT_BUS),
            recipientUserIds = setOf("duty-1"),
        ),
        channel = channel,
    )
}
