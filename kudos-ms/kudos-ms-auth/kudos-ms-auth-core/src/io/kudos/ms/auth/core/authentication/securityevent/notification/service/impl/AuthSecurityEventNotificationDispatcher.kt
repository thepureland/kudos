package io.kudos.ms.auth.core.authentication.securityevent.notification.service.impl

import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationChannelStatusEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationDelivery
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationDispatchResult
import io.kudos.ms.auth.core.authentication.securityevent.notification.event.AuthSecurityEventNotificationChannelDeliveryEvent
import io.kudos.ms.auth.core.authentication.securityevent.notification.event.AuthSecurityEventNotificationChannelOutcomeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.event.AuthSecurityEventNotificationDeliveryEvent
import io.kudos.ms.auth.core.authentication.securityevent.notification.event.AuthSecurityEventNotificationDeliveryOutcomeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationChannelEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationDestinationEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationPublication
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationRoute
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.IAuthSecurityEventNotificationRoutePolicy
import io.kudos.ms.auth.core.authentication.securityevent.notification.service.iservice.IAuthSecurityEventNotificationDispatcher
import io.kudos.ms.auth.core.authentication.securityevent.notification.service.iservice.IAuthSecurityEventNotificationService
import io.kudos.ms.auth.core.authentication.securityevent.notification.spi.AuthSecurityEventNotificationPublishException
import io.kudos.ms.auth.core.authentication.securityevent.notification.spi.IAuthSecurityEventNotificationPublisher
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

/**
 * Claims durable rows, publishes each channel outside a database transaction, then settles them independently.
 *
 * **Why the channel is the unit of delivery.** A route can name several channels, and they fail for unrelated
 * reasons; treating the row as the unit meant one channel's outage re-sent every other channel on the next
 * attempt. Each channel is now settled on its own in the ledger, so a retry only picks up what has not been
 * delivered, and a channel that is permanently refused stops consuming attempts without taking the rest down
 * with it.
 *
 * Delivery remains at-least-once: a worker that dies between a successful send and its settle will attempt
 * that channel again, which is why adapters must stay idempotent on notification id plus channel.
 */
@Service
open class AuthSecurityEventNotificationDispatcher(
    private val notificationService: IAuthSecurityEventNotificationService,
    private val publishers: List<IAuthSecurityEventNotificationPublisher>,
    private val routePolicy: IAuthSecurityEventNotificationRoutePolicy,
    private val eventPublisher: ApplicationEventPublisher,
) : IAuthSecurityEventNotificationDispatcher {

    override fun dispatchPending(workerId: String, limit: Int): AuthSecurityEventNotificationDispatchResult {
        requirePublishers()
        val claimed = notificationService.claimPending(workerId, limit)
        var delivered = 0
        var retryScheduled = 0
        var dead = 0
        var leaseLost = 0
        var channelDelivered = 0
        var channelDead = 0
        claimed.forEach { notification ->
            val route = try {
                validateRoute(routePolicy.resolve(notification))
            } catch (_: AuthSecurityEventException) {
                if (notificationService.dead(notification.id, workerId, ROUTE_INVALID)) {
                    dead++
                    record(notification, AuthSecurityEventNotificationDeliveryOutcomeEnum.DEAD)
                } else {
                    leaseLost++
                    record(notification, AuthSecurityEventNotificationDeliveryOutcomeEnum.LEASE_LOST)
                }
                return@forEach
            } catch (_: Exception) {
                if (notificationService.fail(notification.id, workerId, ROUTE_RESOLUTION_FAILED)) {
                    retryScheduled++
                    record(notification, AuthSecurityEventNotificationDeliveryOutcomeEnum.RETRY_SCHEDULED)
                } else {
                    leaseLost++
                    record(notification, AuthSecurityEventNotificationDeliveryOutcomeEnum.LEASE_LOST)
                }
                return@forEach
            }
            val attempt = deliverChannels(notification, route, workerId)
            channelDelivered += attempt.delivered
            channelDead += attempt.dead
            val settled = notificationService.settledChannels(notification.id)
            val anyDelivered = settled.any {
                it.status == AuthSecurityEventNotificationChannelStatusEnum.DELIVERED
            }
            when {
                // Something is still worth another attempt: back off and keep the unsettled channels pending.
                attempt.retryable -> {
                    val code = attempt.retryErrorCode ?: PUBLISH_FAILED
                    if (notificationService.fail(notification.id, workerId, code)) {
                        retryScheduled++
                        record(notification, AuthSecurityEventNotificationDeliveryOutcomeEnum.RETRY_SCHEDULED)
                    } else {
                        leaseLost++
                        record(notification, AuthSecurityEventNotificationDeliveryOutcomeEnum.LEASE_LOST)
                    }
                }
                // At least one channel reached its recipient; channels permanently refused stay recorded as
                // such in the ledger rather than holding the whole notification open.
                anyDelivered -> {
                    if (notificationService.complete(notification.id, workerId)) {
                        delivered++
                        record(notification, AuthSecurityEventNotificationDeliveryOutcomeEnum.DELIVERED)
                    } else {
                        leaseLost++
                        record(notification, AuthSecurityEventNotificationDeliveryOutcomeEnum.LEASE_LOST)
                    }
                }
                // Every channel was refused permanently: there is nothing left to try, so say so.
                else -> {
                    val code = attempt.deadErrorCode ?: ROUTE_INVALID
                    if (notificationService.dead(notification.id, workerId, code)) {
                        dead++
                        record(notification, AuthSecurityEventNotificationDeliveryOutcomeEnum.DEAD)
                    } else {
                        leaseLost++
                        record(notification, AuthSecurityEventNotificationDeliveryOutcomeEnum.LEASE_LOST)
                    }
                }
            }
        }
        return AuthSecurityEventNotificationDispatchResult(
            claimedCount = claimed.size,
            deliveredCount = delivered,
            retryScheduledCount = retryScheduled,
            deadCount = dead,
            channelDeliveredCount = channelDelivered,
            channelDeadCount = channelDead,
            leaseLostCount = leaseLost,
        )
    }

    /**
     * Attempts every channel the ledger has not settled yet.
     *
     * A retryable failure does not abandon the remaining channels: they are independent, and sending them now
     * is one fewer round of retries for everyone.
     */
    private fun deliverChannels(
        notification: AuthSecurityEventNotificationDelivery,
        route: AuthSecurityEventNotificationRoute,
        workerId: String,
    ): ChannelAttempt {
        val settled = notificationService.settledChannels(notification.id).map { it.channel }.toSet()
        var attempt = ChannelAttempt()
        AuthSecurityEventNotificationChannelEnum.entries
            .filter { it in route.channels && it !in settled }
            .forEach { channel ->
                val publisher = selectPublisher(route.destination, channel)
                if (publisher == null) {
                    settle(notification, channel, AuthSecurityEventNotificationChannelStatusEnum.DEAD, NO_PUBLISHER)
                    attempt = attempt.copy(dead = attempt.dead + 1, deadErrorCode = NO_PUBLISHER)
                    return@forEach
                }
                try {
                    publisher.publish(
                        AuthSecurityEventNotificationPublication(
                            notification = notification,
                            route = route,
                            channel = channel,
                        )
                    )
                } catch (e: AuthSecurityEventNotificationPublishException) {
                    val errorCode = safeErrorCode(e.errorCode)
                    if (e.retryable) {
                        recordChannel(
                            notification,
                            channel,
                            AuthSecurityEventNotificationChannelOutcomeEnum.RETRY_SCHEDULED,
                        )
                        attempt = attempt.copy(retryable = true, retryErrorCode = errorCode)
                    } else {
                        settle(
                            notification,
                            channel,
                            AuthSecurityEventNotificationChannelStatusEnum.DEAD,
                            errorCode,
                        )
                        attempt = attempt.copy(dead = attempt.dead + 1, deadErrorCode = errorCode)
                    }
                    return@forEach
                } catch (e: Exception) {
                    logger.warn(
                        "Security-event notification publisher failed for channel={} worker={}",
                        channel,
                        workerId,
                        e,
                    )
                    recordChannel(notification, channel, AuthSecurityEventNotificationChannelOutcomeEnum.RETRY_SCHEDULED)
                    attempt = attempt.copy(retryable = true, retryErrorCode = PUBLISH_FAILED)
                    return@forEach
                }
                settle(notification, channel, AuthSecurityEventNotificationChannelStatusEnum.DELIVERED, null)
                attempt = attempt.copy(delivered = attempt.delivered + 1)
            }
        return attempt
    }

    /**
     * Exactly one publisher may claim a destination and channel.
     *
     * Two claiming the same pair is a deployment mistake whose effect — a message silently going out through
     * whichever bean happened to be first — is worse than refusing to dispatch, so it is raised rather than
     * resolved.
     */
    private fun selectPublisher(
        destination: AuthSecurityEventNotificationDestinationEnum,
        channel: AuthSecurityEventNotificationChannelEnum,
    ): IAuthSecurityEventNotificationPublisher? {
        val candidates = publishers.filter { it.supports(destination, channel) }
        if (candidates.size > 1) fail("AUTH_SECURITY_EVENT_NOTIFICATION_PUBLISHER_AMBIGUOUS")
        return candidates.singleOrNull()
    }

    private fun settle(
        notification: AuthSecurityEventNotificationDelivery,
        channel: AuthSecurityEventNotificationChannelEnum,
        status: AuthSecurityEventNotificationChannelStatusEnum,
        errorCode: String?,
    ) {
        notificationService.settleChannel(
            notificationId = notification.id,
            tenantId = notification.tenantId,
            channel = channel,
            status = status,
            attemptCount = notification.attemptCount,
            errorCode = errorCode,
        )
        recordChannel(
            notification,
            channel,
            when (status) {
                AuthSecurityEventNotificationChannelStatusEnum.DELIVERED ->
                    AuthSecurityEventNotificationChannelOutcomeEnum.DELIVERED
                AuthSecurityEventNotificationChannelStatusEnum.DEAD ->
                    AuthSecurityEventNotificationChannelOutcomeEnum.DEAD
            },
        )
    }

    private fun requirePublishers() {
        if (publishers.isEmpty()) fail("AUTH_SECURITY_EVENT_NOTIFICATION_PUBLISHER_REQUIRED")
    }

    private fun safeErrorCode(value: String): String = value.trim().takeIf(ERROR_CODE::matches) ?: PUBLISH_FAILED

    private fun validateRoute(route: AuthSecurityEventNotificationRoute): AuthSecurityEventNotificationRoute {
        if (!ROUTE_CODE.matches(route.routeCode) || route.channels.isEmpty() ||
            route.recipientUserIds.size > MAX_ROUTE_RECIPIENTS ||
            route.recipientUserIds.any { it.isBlank() || !IDENTIFIER.matches(it) } ||
            (route.destination == AuthSecurityEventNotificationDestinationEnum.USER && route.recipientUserIds.isEmpty())
        ) {
            fail("AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_INVALID")
        }
        return route
    }

    private fun record(
        notification: AuthSecurityEventNotificationDelivery,
        outcome: AuthSecurityEventNotificationDeliveryOutcomeEnum,
    ) {
        runCatching {
            eventPublisher.publishEvent(AuthSecurityEventNotificationDeliveryEvent(notification.notificationType, outcome))
        }.onFailure {
            logger.warn("Security-event notification delivery observer failed for outcome={}", outcome, it)
        }
    }

    private fun recordChannel(
        notification: AuthSecurityEventNotificationDelivery,
        channel: AuthSecurityEventNotificationChannelEnum,
        outcome: AuthSecurityEventNotificationChannelOutcomeEnum,
    ) {
        runCatching {
            eventPublisher.publishEvent(
                AuthSecurityEventNotificationChannelDeliveryEvent(
                    notification.notificationType,
                    channel,
                    outcome,
                )
            )
        }.onFailure {
            logger.warn("Security-event notification channel observer failed for channel={}", channel, it)
        }
    }

    private fun fail(errorCode: String): Nothing = throw AuthSecurityEventException(errorCode)

    private data class ChannelAttempt(
        val delivered: Int = 0,
        val dead: Int = 0,
        val retryable: Boolean = false,
        val retryErrorCode: String? = null,
        val deadErrorCode: String? = null,
    )

    private companion object {
        const val PUBLISH_FAILED = "AUTH_SECURITY_EVENT_NOTIFICATION_PUBLISH_FAILED"
        const val ROUTE_INVALID = "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_INVALID"
        const val ROUTE_RESOLUTION_FAILED = "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_RESOLUTION_FAILED"
        const val NO_PUBLISHER = "AUTH_SECURITY_EVENT_NOTIFICATION_CHANNEL_NO_PUBLISHER"
        const val MAX_ROUTE_RECIPIENTS = 500
        val ERROR_CODE = Regex("^[A-Za-z0-9_.:-]{1,64}$")
        val ROUTE_CODE = Regex("^[A-Z0-9_.:-]{1,64}$")
        val IDENTIFIER = Regex("^[^\\p{Cntrl}]{1,36}$")
        val logger = LoggerFactory.getLogger(AuthSecurityEventNotificationDispatcher::class.java)
    }
}
