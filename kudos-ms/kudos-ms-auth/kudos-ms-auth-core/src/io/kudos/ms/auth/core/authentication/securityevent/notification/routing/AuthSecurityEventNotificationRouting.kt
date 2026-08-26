package io.kudos.ms.auth.core.authentication.securityevent.notification.routing

import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationDelivery

enum class AuthSecurityEventNotificationDestinationEnum {
    USER,
    TENANT_SECURITY_QUEUE,
}

enum class AuthSecurityEventNotificationChannelEnum {
    SITE_MESSAGE,
    EMAIL,
    SMS,
    IM,
    WEBHOOK,
    WORK_ORDER,
    EVENT_BUS,
}

data class AuthSecurityEventNotificationRoute(
    val routeCode: String,
    val destination: AuthSecurityEventNotificationDestinationEnum,
    val channels: Set<AuthSecurityEventNotificationChannelEnum>,
    val recipientUserIds: Set<String>,
)

/**
 * One notification, one channel.
 *
 * [route] stays whole so a publisher can see the destination, route code and recipients it is delivering for,
 * but [channel] — not `route.channels` — is what this call must send on.
 */
data class AuthSecurityEventNotificationPublication(
    val notification: AuthSecurityEventNotificationDelivery,
    val route: AuthSecurityEventNotificationRoute,
    val channel: AuthSecurityEventNotificationChannelEnum,
)

/** Resolves a route for each attempt, allowing tenant policy changes to take effect after retry or replay. */
fun interface IAuthSecurityEventNotificationRoutePolicy {
    fun resolve(notification: AuthSecurityEventNotificationDelivery): AuthSecurityEventNotificationRoute
}

/** Safe baseline: assigned incidents use the in-site channel; unassigned incidents target a security queue/event bus. */
open class DefaultAuthSecurityEventNotificationRoutePolicy : IAuthSecurityEventNotificationRoutePolicy {
    override fun resolve(notification: AuthSecurityEventNotificationDelivery): AuthSecurityEventNotificationRoute =
        notification.recipientUserId?.let { recipient ->
            AuthSecurityEventNotificationRoute(
                routeCode = ASSIGNEE_ROUTE,
                destination = AuthSecurityEventNotificationDestinationEnum.USER,
                channels = setOf(AuthSecurityEventNotificationChannelEnum.SITE_MESSAGE),
                recipientUserIds = setOf(recipient),
            )
        } ?: AuthSecurityEventNotificationRoute(
            routeCode = TENANT_SECURITY_QUEUE_ROUTE,
            destination = AuthSecurityEventNotificationDestinationEnum.TENANT_SECURITY_QUEUE,
            channels = setOf(AuthSecurityEventNotificationChannelEnum.EVENT_BUS),
            recipientUserIds = emptySet(),
        )

    companion object {
        const val ASSIGNEE_ROUTE = "ASSIGNEE"
        const val TENANT_SECURITY_QUEUE_ROUTE = "TENANT_SECURITY_QUEUE"
    }
}
