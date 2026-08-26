package io.kudos.ms.auth.core.authentication.securityevent.notification.spi

import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationChannelEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationDestinationEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationPublication

/**
 * Reliable hand-off boundary for one durable security-event notification **on one channel**.
 *
 * A deployment registers as many publishers as it has delivery technologies and each declares what it can
 * carry; the dispatcher picks per channel and keeps the per-channel ledger. That replaces the previous
 * arrangement where a multi-channel deployment had to supply a single composite publisher and invent its own
 * partial-delivery bookkeeping — the part that is easy to get wrong and identical in every deployment.
 *
 * Implementations must still be idempotent on `publication.notification.id` together with
 * `publication.channel`: the ledger prevents a re-attempt after a channel is settled, but a worker that dies
 * between a successful send and its settle will come back and try that channel again.
 */
interface IAuthSecurityEventNotificationPublisher {

    /**
     * Whether this publisher can carry the given route.
     *
     * Answering `true` for a channel makes this publisher the only one allowed to carry it: two publishers
     * claiming the same destination and channel is a deployment error and is reported as one rather than
     * resolved by picking arbitrarily.
     */
    fun supports(
        destination: AuthSecurityEventNotificationDestinationEnum,
        channel: AuthSecurityEventNotificationChannelEnum,
    ): Boolean = true

    /** Delivers exactly the channel named by [AuthSecurityEventNotificationPublication.channel]. */
    fun publish(publication: AuthSecurityEventNotificationPublication)
}

/** A stable, non-sensitive failure code controls whether the outbox retries or moves directly to DEAD. */
class AuthSecurityEventNotificationPublishException(
    val errorCode: String,
    val retryable: Boolean = true,
    cause: Throwable? = null,
) : RuntimeException(errorCode, cause)
