package io.kudos.ms.auth.core.authentication.securityevent

import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationChannelOutcome
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationChannelStatusEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationDelivery
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationTypeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.event.AuthSecurityEventNotificationChannelDeliveryEvent
import io.kudos.ms.auth.core.authentication.securityevent.notification.event.AuthSecurityEventNotificationChannelOutcomeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.event.AuthSecurityEventNotificationDeliveryEvent
import io.kudos.ms.auth.core.authentication.securityevent.notification.event.AuthSecurityEventNotificationDeliveryOutcomeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationChannelEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationDestinationEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationPublication
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRoute
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.IAuthSecurityEventNotificationRoutePolicy
import io.kudos.ms.auth.core.authentication.securityevent.notification.service.impl.AuthSecurityEventNotificationDispatcher
import io.kudos.ms.auth.core.authentication.securityevent.notification.service.iservice.IAuthSecurityEventNotificationService
import io.kudos.ms.auth.core.authentication.securityevent.notification.spi.AuthSecurityEventNotificationPublishException
import io.kudos.ms.auth.core.authentication.securityevent.notification.spi.IAuthSecurityEventNotificationPublisher
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class AuthSecurityEventNotificationDispatcherTest {
    private val eventPublisher = mock(ApplicationEventPublisher::class.java)

    @Test
    fun everyChannelOfARouteIsDeliveredAndSettledIndependently() {
        val service = FakeNotificationService(listOf(delivery("notification-1")))
        val publisher = TestPublisher()

        val result = dispatcher(service, publisher, route(SITE_MESSAGE, EMAIL)).dispatchPending("worker-1", 10)

        assertEquals(1, result.deliveredCount)
        assertEquals(2, result.channelDeliveredCount)
        assertEquals(0, result.channelDeadCount)
        assertEquals(listOf(SITE_MESSAGE, EMAIL), publisher.attempted)
        assertEquals(listOf("notification-1"), service.completed)
        assertEquals(
            setOf(SITE_MESSAGE, EMAIL),
            service.settled.getValue("notification-1").map { it.channel }.toSet(),
        )
        verify(eventPublisher).publishEvent(
            AuthSecurityEventNotificationChannelDeliveryEvent(
                AuthSecurityEventNotificationTypeEnum.SLA_ESCALATED,
                EMAIL,
                AuthSecurityEventNotificationChannelOutcomeEnum.DELIVERED,
            )
        )
        verify(eventPublisher).publishEvent(
            AuthSecurityEventNotificationDeliveryEvent(
                AuthSecurityEventNotificationTypeEnum.SLA_ESCALATED,
                AuthSecurityEventNotificationDeliveryOutcomeEnum.DELIVERED,
            )
        )
    }

    @Test
    fun aRetryDoesNotResendTheChannelThatAlreadyArrived() {
        val service = FakeNotificationService(listOf(delivery("notification-1")))
        val publisher = TestPublisher { channel ->
            if (channel == EMAIL) throw AuthSecurityEventNotificationPublishException("CHANNEL_TIMEOUT")
        }
        val dispatcher = dispatcher(service, publisher, route(SITE_MESSAGE, EMAIL))

        val first = dispatcher.dispatchPending("worker-1", 10)

        assertEquals(1, first.retryScheduledCount)
        assertEquals(1, first.channelDeliveredCount)
        assertEquals(listOf("notification-1" to "CHANNEL_TIMEOUT"), service.failed)
        assertEquals(listOf(SITE_MESSAGE), service.settled.getValue("notification-1").map { it.channel })

        publisher.behaviour = {}
        val second = dispatcher.dispatchPending("worker-1", 10)

        assertEquals(1, second.deliveredCount)
        // The already-delivered channel is attempted once in total; only the failed one comes back.
        assertEquals(listOf(SITE_MESSAGE, EMAIL, EMAIL), publisher.attempted)
        assertEquals(listOf("notification-1"), service.completed)
    }

    @Test
    fun aChannelNoPublisherClaimsIsSettledDeadWithoutHoldingBackTheOthers() {
        val service = FakeNotificationService(listOf(delivery("notification-1")))
        val publisher = TestPublisher(supported = setOf(SITE_MESSAGE))

        val result = dispatcher(service, publisher, route(SITE_MESSAGE, EVENT_BUS)).dispatchPending("worker-1", 10)

        assertEquals(1, result.deliveredCount)
        assertEquals(1, result.channelDeliveredCount)
        assertEquals(1, result.channelDeadCount)
        assertEquals(listOf(SITE_MESSAGE), publisher.attempted)
        val settled = service.settled.getValue("notification-1").associateBy { it.channel }
        assertEquals(
            AuthSecurityEventNotificationChannelStatusEnum.DEAD,
            settled.getValue(EVENT_BUS).status,
        )
        assertEquals(
            "AUTH_SECURITY_EVENT_NOTIFICATION_CHANNEL_NO_PUBLISHER",
            settled.getValue(EVENT_BUS).lastErrorCode,
        )
        assertEquals(listOf("notification-1"), service.completed)
    }

    @Test
    fun aRowOnlyDiesWhenEveryChannelWasRefusedPermanently() {
        val service = FakeNotificationService(listOf(delivery("notification-1")))
        val publisher = TestPublisher {
            throw AuthSecurityEventNotificationPublishException("INVALID_DESTINATION", retryable = false)
        }

        val result = dispatcher(service, publisher, route(SITE_MESSAGE, EMAIL)).dispatchPending("worker-1", 10)

        assertEquals(1, result.deadCount)
        assertEquals(2, result.channelDeadCount)
        assertEquals(listOf("notification-1" to "INVALID_DESTINATION"), service.deadRows)
        assertTrue(
            service.settled.getValue("notification-1").all {
                it.status == AuthSecurityEventNotificationChannelStatusEnum.DEAD
            }
        )
    }

    @Test
    fun anUnknownPublisherFailurePersistsOnlyAStableCodeAndRetries() {
        val service = FakeNotificationService(listOf(delivery("notification-1")))
        val publisher = TestPublisher { error("provider detail must not be persisted") }

        val result = dispatcher(service, publisher, route(SITE_MESSAGE)).dispatchPending("worker-1", 10)

        assertEquals(1, result.retryScheduledCount)
        assertEquals(
            listOf("notification-1" to "AUTH_SECURITY_EVENT_NOTIFICATION_PUBLISH_FAILED"),
            service.failed,
        )
        assertEquals(emptyList(), service.settled["notification-1"].orEmpty())
    }

    @Test
    fun aLostLeaseIsReportedInsteadOfBeingCountedAsDelivered() {
        val service = FakeNotificationService(listOf(delivery("notification-1")), completeResult = false)

        val result = dispatcher(service, TestPublisher(), route(SITE_MESSAGE)).dispatchPending("worker-1", 10)

        assertEquals(0, result.deliveredCount)
        assertEquals(1, result.leaseLostCount)
    }

    @Test
    fun publishersMustExistAndMayNotOverlapOnTheSameChannel() {
        val service = FakeNotificationService(listOf(delivery("notification-1")))
        val missing = AuthSecurityEventNotificationDispatcher(
            service,
            emptyList(),
            policy(route(SITE_MESSAGE)),
            eventPublisher,
        )
        val overlapping = AuthSecurityEventNotificationDispatcher(
            service,
            listOf(TestPublisher(), TestPublisher()),
            policy(route(SITE_MESSAGE)),
            eventPublisher,
        )

        assertEquals(
            "AUTH_SECURITY_EVENT_NOTIFICATION_PUBLISHER_REQUIRED",
            assertFailsWith<AuthSecurityEventException> { missing.dispatchPending("worker-1") }.errorCode,
        )
        assertEquals(
            "AUTH_SECURITY_EVENT_NOTIFICATION_PUBLISHER_AMBIGUOUS",
            assertFailsWith<AuthSecurityEventException> { overlapping.dispatchPending("worker-1") }.errorCode,
        )
    }

    @Test
    fun invalidTenantRouteMovesTheClaimedRowDirectlyToDeadWithoutCallingPublisher() {
        val service = FakeNotificationService(listOf(delivery("notification-1")))
        val publisher = TestPublisher()
        val invalidRoute = AuthSecurityEventNotificationRoute(
            routeCode = "invalid lowercase",
            destination = AuthSecurityEventNotificationDestinationEnum.USER,
            channels = setOf(SITE_MESSAGE),
            recipientUserIds = setOf("admin-2"),
        )

        val result = dispatcher(service, publisher, invalidRoute).dispatchPending("worker-1", 1)

        assertEquals(1, result.deadCount)
        assertEquals(emptyList(), publisher.attempted)
        assertEquals(
            listOf("notification-1" to "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_INVALID"),
            service.deadRows,
        )
    }

    private fun dispatcher(
        service: IAuthSecurityEventNotificationService,
        publisher: IAuthSecurityEventNotificationPublisher,
        route: AuthSecurityEventNotificationRoute,
    ) = AuthSecurityEventNotificationDispatcher(service, listOf(publisher), policy(route), eventPublisher)

    private fun policy(route: AuthSecurityEventNotificationRoute) =
        IAuthSecurityEventNotificationRoutePolicy { route }

    private fun route(vararg channels: AuthSecurityEventNotificationChannelEnum) =
        AuthSecurityEventNotificationRoute(
            routeCode = "ASSIGNEE",
            destination = AuthSecurityEventNotificationDestinationEnum.USER,
            channels = channels.toSet(),
            recipientUserIds = setOf("admin-2"),
        )

    private fun delivery(id: String) = AuthSecurityEventNotificationDelivery(
        id = id,
        tenantId = "tenant-1",
        eventId = "event-1",
        notificationType = AuthSecurityEventNotificationTypeEnum.SLA_ESCALATED,
        escalationLevel = 1,
        recipientUserId = "admin-2",
        dueAt = LocalDateTime.parse("2026-08-25T09:00:00"),
        escalatedAt = LocalDateTime.parse("2026-08-25T10:00:00"),
        attemptCount = 1,
        leaseUntil = LocalDateTime.parse("2026-08-25T10:01:00"),
    )

    /** Records what it was asked to carry; `behaviour` decides how each channel fails. */
    private class TestPublisher(
        private val supported: Set<AuthSecurityEventNotificationChannelEnum> =
            AuthSecurityEventNotificationChannelEnum.entries.toSet(),
        var behaviour: (AuthSecurityEventNotificationChannelEnum) -> Unit = {},
    ) : IAuthSecurityEventNotificationPublisher {
        val attempted = mutableListOf<AuthSecurityEventNotificationChannelEnum>()

        override fun supports(
            destination: AuthSecurityEventNotificationDestinationEnum,
            channel: AuthSecurityEventNotificationChannelEnum,
        ): Boolean = channel in supported

        override fun publish(publication: AuthSecurityEventNotificationPublication) {
            attempted += publication.channel
            behaviour(publication.channel)
        }
    }

    /**
     * An in-memory outbox with a real ledger.
     *
     * A plain mock cannot express "the ledger now contains what the dispatcher just settled", which is exactly
     * the behaviour under test, so the fake keeps the state instead.
     */
    private class FakeNotificationService(
        private val claimable: List<AuthSecurityEventNotificationDelivery>,
        private val completeResult: Boolean = true,
    ) : IAuthSecurityEventNotificationService {
        val settled = mutableMapOf<String, MutableList<AuthSecurityEventNotificationChannelOutcome>>()
        val completed = mutableListOf<String>()
        val failed = mutableListOf<Pair<String, String>>()
        val deadRows = mutableListOf<Pair<String, String>>()

        override fun claimPending(workerId: String, limit: Int) = claimable

        override fun settledChannels(notificationId: String) = settled[notificationId].orEmpty().toList()

        override fun settleChannel(
            notificationId: String,
            tenantId: String,
            channel: AuthSecurityEventNotificationChannelEnum,
            status: AuthSecurityEventNotificationChannelStatusEnum,
            attemptCount: Int,
            errorCode: String?,
        ): Boolean {
            val outcomes = settled.getOrPut(notificationId) { mutableListOf() }
            if (outcomes.any { it.channel == channel }) return false
            outcomes += AuthSecurityEventNotificationChannelOutcome(
                channel = channel,
                status = status,
                attemptCount = attemptCount,
                lastErrorCode = errorCode,
                settledAt = LocalDateTime.parse("2026-08-25T10:00:00"),
            )
            return true
        }

        override fun complete(notificationId: String, workerId: String): Boolean {
            if (!completeResult) return false
            completed += notificationId
            return true
        }

        override fun fail(notificationId: String, workerId: String, errorCode: String): Boolean {
            failed += notificationId to errorCode
            return true
        }

        override fun dead(notificationId: String, workerId: String, errorCode: String): Boolean {
            deadRows += notificationId to errorCode
            return true
        }
    }

    private companion object {
        val SITE_MESSAGE = AuthSecurityEventNotificationChannelEnum.SITE_MESSAGE
        val EMAIL = AuthSecurityEventNotificationChannelEnum.EMAIL
        val EVENT_BUS = AuthSecurityEventNotificationChannelEnum.EVENT_BUS
    }
}
