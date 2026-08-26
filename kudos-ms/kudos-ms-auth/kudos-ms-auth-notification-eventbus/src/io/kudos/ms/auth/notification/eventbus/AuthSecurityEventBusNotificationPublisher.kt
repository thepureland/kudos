package io.kudos.ms.auth.notification.eventbus

import io.kudos.ability.distributed.stream.common.support.StreamProducerHelper
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationChannelEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationDestinationEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationPublication
import io.kudos.ms.auth.core.authentication.securityevent.notification.spi.AuthSecurityEventNotificationPublishException
import io.kudos.ms.auth.core.authentication.securityevent.notification.spi.IAuthSecurityEventNotificationPublisher

/**
 * Hands the `EVENT_BUS` channel to whichever broker the deployment bound through
 * `kudos-ability-distributed-stream`, which is what makes the built-in tenant security-queue route deliverable
 * without core depending on Kafka, RabbitMQ or RocketMQ.
 *
 * **What a successful send means here.** Spring Cloud Stream's `StreamBridge` is asynchronous: a `true` return
 * says the message entered the local producer queue, not that a broker acknowledged it. A later flush failure
 * is surfaced on the stream error channel and persisted by `kudos-ability-distributed-stream` into
 * `sys_mq_fail_msg`. So responsibility genuinely transfers — but to *that* ledger, not to the auth outbox,
 * which has already recorded the channel as delivered. A deployment that leaves the stream failure persistence
 * switched off turns a broker outage into a silently dropped notification; the module cannot detect that on
 * its behalf, so it is stated plainly here and in the README rather than implied to be safe.
 */
open class AuthSecurityEventBusNotificationPublisher(
    private val producerHelper: StreamProducerHelper,
    private val properties: AuthSecurityEventBusNotificationProperties,
) : IAuthSecurityEventNotificationPublisher {

    /**
     * Only the bus channel, and every destination.
     *
     * Both a tenant security queue and a user-targeted route may legitimately be carried by a bus, but
     * claiming `WORK_ORDER` or the in-app channels would mean quietly swallowing notifications this adapter
     * has no way to deliver.
     */
    override fun supports(
        destination: AuthSecurityEventNotificationDestinationEnum,
        channel: AuthSecurityEventNotificationChannelEnum,
    ): Boolean = channel == AuthSecurityEventNotificationChannelEnum.EVENT_BUS

    override fun publish(publication: AuthSecurityEventNotificationPublication) {
        val bindingName = properties.bindingName.trim()
        if (!BINDING_NAME.matches(bindingName)) fail(CONFIG_INVALID, retryable = false)
        if (publication.channel != AuthSecurityEventNotificationChannelEnum.EVENT_BUS) {
            fail(CHANNEL_UNSUPPORTED, retryable = false)
        }
        val notification = publication.notification
        val route = publication.route
        val message = AuthSecurityEventBusNotificationMessage(
            idempotencyKey = "${notification.id}:${publication.channel.name}",
            notificationId = notification.id,
            tenantId = notification.tenantId,
            eventId = notification.eventId,
            notificationType = notification.notificationType.name,
            escalationLevel = notification.escalationLevel,
            attemptCount = notification.attemptCount,
            dueAt = notification.dueAt,
            escalatedAt = notification.escalatedAt,
            routeCode = route.routeCode,
            destination = route.destination.name,
            channel = publication.channel.name,
            recipientUserIds = route.recipientUserIds,
        )
        val accepted = try {
            producerHelper.sendMessage(bindingName, message)
        } catch (e: Exception) {
            throw AuthSecurityEventNotificationPublishException(BUS_UNAVAILABLE, retryable = true, cause = e)
        }
        // `false` covers a missing binding, producer backpressure and a refused send alike, and the helper does
        // not distinguish them. Retryable is the safe reading: backpressure clears on its own, and a binding
        // that was never configured surfaces as the notification exhausting its attempts and landing in the
        // dead-letter list with a stable code, where an operator can see it.
        if (!accepted) fail(BUS_NOT_ACCEPTED, retryable = true)
    }

    private fun fail(errorCode: String, retryable: Boolean): Nothing =
        throw AuthSecurityEventNotificationPublishException(errorCode, retryable)

    private companion object {
        const val CONFIG_INVALID = "AUTH_SECURITY_EVENT_NOTIFICATION_BUS_CONFIG_INVALID"
        const val CHANNEL_UNSUPPORTED = "AUTH_SECURITY_EVENT_NOTIFICATION_BUS_CHANNEL_UNSUPPORTED"
        const val BUS_UNAVAILABLE = "AUTH_SECURITY_EVENT_NOTIFICATION_BUS_UNAVAILABLE"
        const val BUS_NOT_ACCEPTED = "AUTH_SECURITY_EVENT_NOTIFICATION_BUS_NOT_ACCEPTED"
        val BINDING_NAME = Regex("^[A-Za-z0-9_.-]{1,128}$")
    }
}
