package io.kudos.ms.auth.core.authentication.securityevent.notification.service.iservice

import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationChannelOutcome
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationChannelStatusEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationDelivery
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationChannelEnum

interface IAuthSecurityEventNotificationService {
    fun claimPending(workerId: String, limit: Int = 100): List<AuthSecurityEventNotificationDelivery>

    /** The channels of this notification that already reached a terminal outcome and must not be re-sent. */
    fun settledChannels(notificationId: String): List<AuthSecurityEventNotificationChannelOutcome>

    /**
     * Records one terminal channel outcome.
     *
     * @return true when this call recorded it, false when the channel was already settled by an earlier
     *         attempt — which is the normal answer after a worker died between sending and settling.
     */
    fun settleChannel(
        notificationId: String,
        tenantId: String,
        channel: AuthSecurityEventNotificationChannelEnum,
        status: AuthSecurityEventNotificationChannelStatusEnum,
        attemptCount: Int,
        errorCode: String?,
    ): Boolean

    fun complete(notificationId: String, workerId: String): Boolean

    fun fail(notificationId: String, workerId: String, errorCode: String): Boolean

    fun dead(notificationId: String, workerId: String, errorCode: String): Boolean
}
