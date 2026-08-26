package io.kudos.ms.auth.core.authentication.securityevent.notification.event

import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationTypeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationChannelEnum

enum class AuthSecurityEventNotificationDeliveryOutcomeEnum {
    DELIVERED,
    RETRY_SCHEDULED,
    DEAD,
    LEASE_LOST,
}

/** Low-cardinality operational event; tenant, recipient, route and provider details are deliberately excluded. */
data class AuthSecurityEventNotificationDeliveryEvent(
    val notificationType: AuthSecurityEventNotificationTypeEnum,
    val outcome: AuthSecurityEventNotificationDeliveryOutcomeEnum,
)

/** What one channel of one attempt produced. `RETRY_SCHEDULED` leaves the channel unsettled and re-attempted. */
enum class AuthSecurityEventNotificationChannelOutcomeEnum {
    DELIVERED,
    RETRY_SCHEDULED,
    DEAD,
}

/**
 * Per-channel counterpart of [AuthSecurityEventNotificationDeliveryEvent].
 *
 * The channel is a fixed enum, so this stays bounded: a deployment can alarm on "SMS keeps failing" without
 * any tenant, recipient or vendor error text entering the time series.
 */
data class AuthSecurityEventNotificationChannelDeliveryEvent(
    val notificationType: AuthSecurityEventNotificationTypeEnum,
    val channel: AuthSecurityEventNotificationChannelEnum,
    val outcome: AuthSecurityEventNotificationChannelOutcomeEnum,
)
