package io.kudos.ms.auth.notification.msg

import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationChannelEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationDestinationEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationPublication
import io.kudos.ms.auth.core.authentication.securityevent.notification.spi.AuthSecurityEventNotificationPublishException
import io.kudos.ms.auth.core.authentication.securityevent.notification.spi.IAuthSecurityEventNotificationPublisher
import io.kudos.ms.msg.common.send.api.IMsgSendApi
import io.kudos.ms.msg.common.send.enums.MsgPublishMethodEnum
import io.kudos.ms.msg.common.send.vo.request.MsgPublishRequest

/**
 * Durable hand-off from the auth outbox to kudos-ms-msg for one channel.
 *
 * Declares only what kudos-ms-msg can actually carry, so a deployment can register this alongside publishers
 * for the queue-facing channels instead of wrapping both in a hand-written composite.
 */
open class AuthSecurityEventMsgNotificationPublisher(
    private val msgSendApi: IMsgSendApi,
    private val properties: AuthSecurityEventMsgNotificationProperties,
) : IAuthSecurityEventNotificationPublisher {

    override fun supports(
        destination: AuthSecurityEventNotificationDestinationEnum,
        channel: AuthSecurityEventNotificationChannelEnum,
    ): Boolean = destination == AuthSecurityEventNotificationDestinationEnum.USER && channel in CHANNELS

    override fun publish(publication: AuthSecurityEventNotificationPublication) {
        validateProperties()
        val route = publication.route
        if (route.destination != AuthSecurityEventNotificationDestinationEnum.USER || route.recipientUserIds.isEmpty()) {
            fail(ROUTE_UNSUPPORTED, retryable = false)
        }
        val method = CHANNELS[publication.channel] ?: fail(ROUTE_UNSUPPORTED, retryable = false)
        val notification = publication.notification
        val accepted = try {
            msgSendApi.publish(
                MsgPublishRequest(
                    tenantId = notification.tenantId,
                    eventTypeDictCode = properties.eventTypeDictCode,
                    msgTypeDictCode = properties.msgTypeDictCode,
                    publishMethod = method,
                    receiverIds = route.recipientUserIds,
                    params = mapOf(
                        "notificationId" to notification.id,
                        "eventId" to notification.eventId,
                        "notificationType" to notification.notificationType.name,
                        "escalationLevel" to notification.escalationLevel.toString(),
                        "dueAt" to (notification.dueAt?.toString() ?: ""),
                        "escalatedAt" to notification.escalatedAt.toString(),
                        "routeCode" to route.routeCode,
                    ),
                    localeDictCode = properties.localeDictCode,
                    // Unchanged from the multi-channel form: core now calls once per channel, but the key a
                    // downstream send already recorded must keep matching on a retry.
                    idempotencyKey = "${notification.id}:${method.dictCode}",
                )
            )
        } catch (e: Exception) {
            throw AuthSecurityEventNotificationPublishException(MSG_UNAVAILABLE, retryable = true, cause = e)
        }
        if (accepted == null) fail(MSG_NOT_ACCEPTED, retryable = true)
    }

    private fun validateProperties() {
        if (!DICT_CODE.matches(properties.eventTypeDictCode) || !DICT_CODE.matches(properties.msgTypeDictCode) ||
            properties.localeDictCode?.let { !DICT_CODE.matches(it) } == true
        ) {
            fail(CONFIG_INVALID, retryable = false)
        }
    }

    private fun fail(errorCode: String, retryable: Boolean): Nothing =
        throw AuthSecurityEventNotificationPublishException(errorCode, retryable)

    private companion object {
        const val ROUTE_UNSUPPORTED = "AUTH_SECURITY_EVENT_NOTIFICATION_MSG_ROUTE_UNSUPPORTED"
        const val MSG_UNAVAILABLE = "AUTH_SECURITY_EVENT_NOTIFICATION_MSG_UNAVAILABLE"
        const val MSG_NOT_ACCEPTED = "AUTH_SECURITY_EVENT_NOTIFICATION_MSG_NOT_ACCEPTED"
        const val CONFIG_INVALID = "AUTH_SECURITY_EVENT_NOTIFICATION_MSG_CONFIG_INVALID"
        val DICT_CODE = Regex("^[A-Za-z0-9_.:-]{1,64}$")
        val CHANNELS = mapOf(
            AuthSecurityEventNotificationChannelEnum.SITE_MESSAGE to MsgPublishMethodEnum.SITE_MSG,
            AuthSecurityEventNotificationChannelEnum.EMAIL to MsgPublishMethodEnum.EMAIL,
            AuthSecurityEventNotificationChannelEnum.SMS to MsgPublishMethodEnum.SMS,
        )
    }
}
