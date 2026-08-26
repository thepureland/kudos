package io.kudos.ms.auth.notification.msg

import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationDelivery
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationTypeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationChannelEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationDestinationEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationPublication
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRoute
import io.kudos.ms.auth.core.authentication.securityevent.notification.spi.AuthSecurityEventNotificationPublishException
import io.kudos.ms.msg.common.send.api.IMsgSendApi
import io.kudos.ms.msg.common.send.enums.MsgPublishMethodEnum
import io.kudos.ms.msg.common.send.vo.request.MsgPublishRequest
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class AuthSecurityEventMsgNotificationPublisherTest {

    @Test
    fun eachCallCarriesOneChannelWithItsOwnIdempotencyKeyAndStableTemplateParameters() {
        val api = RecordingMsgSendApi { request -> "send-${request.publishMethod.dictCode}" }
        val publisher = AuthSecurityEventMsgNotificationPublisher(api, AuthSecurityEventMsgNotificationProperties())

        publisher.publish(publication(AuthSecurityEventNotificationChannelEnum.SITE_MESSAGE))
        publisher.publish(publication(AuthSecurityEventNotificationChannelEnum.EMAIL))

        assertEquals(
            listOf(MsgPublishMethodEnum.SITE_MSG, MsgPublishMethodEnum.EMAIL),
            api.requests.map { it.publishMethod },
        )
        // Unchanged keys: a send recorded before the per-channel split must still match on a retry.
        assertEquals(
            listOf("notification-1:siteMsg", "notification-1:email"),
            api.requests.map { it.idempotencyKey },
        )
        assertEquals(setOf("admin-2"), api.requests.first().receiverIds)
        assertEquals("event-1", api.requests.first().params["eventId"])
        assertEquals("1", api.requests.first().params["escalationLevel"])
    }

    @Test
    fun onlyUserTargetedInSiteEmailAndSmsChannelsAreClaimed() {
        val publisher = AuthSecurityEventMsgNotificationPublisher(
            RecordingMsgSendApi { "send-1" },
            AuthSecurityEventMsgNotificationProperties(),
        )
        val user = AuthSecurityEventNotificationDestinationEnum.USER
        val queue = AuthSecurityEventNotificationDestinationEnum.TENANT_SECURITY_QUEUE

        assertTrue(publisher.supports(user, AuthSecurityEventNotificationChannelEnum.SITE_MESSAGE))
        assertTrue(publisher.supports(user, AuthSecurityEventNotificationChannelEnum.EMAIL))
        assertTrue(publisher.supports(user, AuthSecurityEventNotificationChannelEnum.SMS))
        assertFalse(publisher.supports(user, AuthSecurityEventNotificationChannelEnum.EVENT_BUS))
        assertFalse(publisher.supports(user, AuthSecurityEventNotificationChannelEnum.WORK_ORDER))
        // The queue route needs a bus or ticketing publisher; claiming it here would silently swallow it.
        assertFalse(publisher.supports(queue, AuthSecurityEventNotificationChannelEnum.SITE_MESSAGE))
    }

    @Test
    fun unsupportedRouteIsPermanentAndRejectedBeforeAnyPartialPublish() {
        val api = RecordingMsgSendApi { "send-1" }
        val publisher = AuthSecurityEventMsgNotificationPublisher(api, AuthSecurityEventMsgNotificationProperties())

        val failure = assertFailsWith<AuthSecurityEventNotificationPublishException> {
            publisher.publish(publication(AuthSecurityEventNotificationChannelEnum.EVENT_BUS))
        }

        assertFalse(failure.retryable)
        assertEquals("AUTH_SECURITY_EVENT_NOTIFICATION_MSG_ROUTE_UNSUPPORTED", failure.errorCode)
        assertEquals(emptyList(), api.requests)
    }

    @Test
    fun missingTemplateFallbackOrTransportFailureRemainsRetryable() {
        val rejected = AuthSecurityEventMsgNotificationPublisher(
            RecordingMsgSendApi { null },
            AuthSecurityEventMsgNotificationProperties(),
        )
        val unavailable = AuthSecurityEventMsgNotificationPublisher(
            RecordingMsgSendApi { error("remote down") },
            AuthSecurityEventMsgNotificationProperties(),
        )

        val rejectedFailure = assertFailsWith<AuthSecurityEventNotificationPublishException> {
            rejected.publish(publication(AuthSecurityEventNotificationChannelEnum.SITE_MESSAGE))
        }
        val unavailableFailure = assertFailsWith<AuthSecurityEventNotificationPublishException> {
            unavailable.publish(publication(AuthSecurityEventNotificationChannelEnum.SITE_MESSAGE))
        }

        assertTrue(rejectedFailure.retryable)
        assertTrue(unavailableFailure.retryable)
        assertEquals("AUTH_SECURITY_EVENT_NOTIFICATION_MSG_NOT_ACCEPTED", rejectedFailure.errorCode)
        assertEquals("AUTH_SECURITY_EVENT_NOTIFICATION_MSG_UNAVAILABLE", unavailableFailure.errorCode)
    }

    private fun publication(channel: AuthSecurityEventNotificationChannelEnum) =
        AuthSecurityEventNotificationPublication(
            notification = AuthSecurityEventNotificationDelivery(
                id = "notification-1",
                tenantId = "tenant-1",
                eventId = "event-1",
                notificationType = AuthSecurityEventNotificationTypeEnum.SLA_ESCALATED,
                escalationLevel = 1,
                recipientUserId = "admin-2",
                dueAt = LocalDateTime.parse("2026-08-25T09:00:00"),
                escalatedAt = LocalDateTime.parse("2026-08-25T10:00:00"),
                attemptCount = 1,
                leaseUntil = LocalDateTime.parse("2026-08-25T10:01:00"),
            ),
            route = AuthSecurityEventNotificationRoute(
                routeCode = "ASSIGNEE",
                destination = AuthSecurityEventNotificationDestinationEnum.USER,
                channels = setOf(channel),
                recipientUserIds = setOf("admin-2"),
            ),
            channel = channel,
        )

    private class RecordingMsgSendApi(
        private val result: (MsgPublishRequest) -> String?,
    ) : IMsgSendApi {
        val requests = mutableListOf<MsgPublishRequest>()

        override fun publish(request: MsgPublishRequest): String? {
            requests += request
            return result(request)
        }
    }
}
