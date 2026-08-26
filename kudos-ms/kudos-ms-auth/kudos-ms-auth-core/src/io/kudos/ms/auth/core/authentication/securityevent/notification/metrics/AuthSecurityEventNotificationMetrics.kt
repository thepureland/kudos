package io.kudos.ms.auth.core.authentication.securityevent.notification.metrics

import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationTypeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.event.AuthSecurityEventNotificationDeliveryEvent
import io.kudos.ms.auth.core.authentication.securityevent.notification.event.AuthSecurityEventNotificationChannelDeliveryEvent
import io.kudos.ms.auth.core.authentication.securityevent.notification.event.AuthSecurityEventNotificationChannelOutcomeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.event.AuthSecurityEventNotificationDeliveryOutcomeEnum
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.AuthSecurityEventNotificationChannelEnum
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.event.EventListener

/** Fixed-cardinality delivery counters; tenant, recipient, route, worker and provider error stay out of metric tags. */
open class AuthSecurityEventNotificationMetrics(registry: MeterRegistry) {
    private val counters: Map<Key, Counter> = AuthSecurityEventNotificationTypeEnum.entries
        .flatMap { type -> AuthSecurityEventNotificationDeliveryOutcomeEnum.entries.map { Key(type, it) } }
        .associateWith { key ->
            Counter.builder(METRIC_NAME)
                .description("Security-event notification delivery outcomes")
                .tag("type", key.type.name)
                .tag("outcome", key.outcome.name)
                .register(registry)
        }

    /**
     * Bounded by construction: notification type × channel × outcome are all fixed enums, so the per-channel
     * series count is a small constant rather than something a tenant or a vendor error can grow.
     */
    private val channelCounters: Map<ChannelKey, Counter> = AuthSecurityEventNotificationTypeEnum.entries
        .flatMap { type ->
            AuthSecurityEventNotificationChannelEnum.entries.flatMap { channel ->
                AuthSecurityEventNotificationChannelOutcomeEnum.entries.map { ChannelKey(type, channel, it) }
            }
        }
        .associateWith { key ->
            Counter.builder(CHANNEL_METRIC_NAME)
                .description("Security-event notification delivery outcomes per channel")
                .tag("type", key.type.name)
                .tag("channel", key.channel.name)
                .tag("outcome", key.outcome.name)
                .register(registry)
        }

    @EventListener
    open fun onDelivery(event: AuthSecurityEventNotificationDeliveryEvent) {
        counters[Key(event.notificationType, event.outcome)]?.increment()
    }

    @EventListener
    open fun onChannelDelivery(event: AuthSecurityEventNotificationChannelDeliveryEvent) {
        channelCounters[ChannelKey(event.notificationType, event.channel, event.outcome)]?.increment()
    }

    private data class Key(
        val type: AuthSecurityEventNotificationTypeEnum,
        val outcome: AuthSecurityEventNotificationDeliveryOutcomeEnum,
    )

    private data class ChannelKey(
        val type: AuthSecurityEventNotificationTypeEnum,
        val channel: AuthSecurityEventNotificationChannelEnum,
        val outcome: AuthSecurityEventNotificationChannelOutcomeEnum,
    )

    companion object {
        const val METRIC_NAME = "kudos.auth.security.event.notification.delivery"
        const val CHANNEL_METRIC_NAME = "kudos.auth.security.event.notification.channel.delivery"
    }
}
