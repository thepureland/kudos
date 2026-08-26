package io.kudos.ms.auth.core.authentication.securityevent.notification.routing

import io.kudos.ms.auth.core.authentication.securityevent.model.AuthSecurityEventException
import io.kudos.ms.auth.core.authentication.securityevent.notification.AuthSecurityEventNotificationDelivery
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.service.iservice.IAuthSecurityEventNotificationRouteConfigService
import io.kudos.ms.auth.core.authentication.securityevent.oncall.spi.IAuthSecurityEventResponderResolver
import java.time.Clock
import java.time.LocalDateTime

/**
 * Resolves each attempt against the tenant's stored rules, falling back to [DefaultAuthSecurityEventNotificationRoutePolicy].
 *
 * Resolution happens per attempt rather than once per outbox row, so a routing change made while a
 * notification is retrying — or before an operator replays a dead one — takes effect on the next attempt
 * without touching the durable row. The same property is what makes a rotation usable here at all: "who is on
 * call" is evaluated at the moment of the attempt, not at the moment the incident escalated.
 *
 * A tenant with nothing configured, or whose rule for this delivery shape is disabled, keeps the pre-V53
 * behaviour exactly; this class never invents a recipient the configuration did not name.
 */
open class PersistentAuthSecurityEventNotificationRoutePolicy(
    private val configService: IAuthSecurityEventNotificationRouteConfigService,
    private val responderResolver: IAuthSecurityEventResponderResolver? = null,
    private val clock: Clock = Clock.systemUTC(),
    private val defaultPolicy: IAuthSecurityEventNotificationRoutePolicy =
        DefaultAuthSecurityEventNotificationRoutePolicy(),
) : IAuthSecurityEventNotificationRoutePolicy {

    override fun resolve(
        notification: AuthSecurityEventNotificationDelivery,
    ): AuthSecurityEventNotificationRoute {
        val appliesTo = if (notification.recipientUserId == null) {
            AuthSecurityEventNotificationRouteAppliesToEnum.UNASSIGNED
        } else {
            AuthSecurityEventNotificationRouteAppliesToEnum.ASSIGNED
        }
        val config = configService.findEffective(notification.tenantId, notification.notificationType, appliesTo)
            ?: return defaultPolicy.resolve(notification)
        val recipients = buildSet {
            addAll(config.responderUserIds)
            if (config.includeAssignee) notification.recipientUserId?.let(::add)
            addAll(onCallResponders(config, notification))
        }
        if (config.destination == AuthSecurityEventNotificationDestinationEnum.USER && recipients.isEmpty()) {
            return fallback(config, notification)
        }
        return AuthSecurityEventNotificationRoute(
            routeCode = config.routeCode,
            destination = config.destination,
            channels = config.channels,
            recipientUserIds = recipients,
        )
    }

    /**
     * A rule can name a rotation without the deployment having one: an empty resolver means the rotation
     * contributes nobody, which the fallback then handles like any other unaddressable route. A resolver that
     * *throws* is a different thing — the rotation could not be read — and is deliberately allowed to
     * propagate so the dispatcher retries instead of recording a routing decision made without the roster.
     */
    private fun onCallResponders(
        config: AuthSecurityEventNotificationRouteConfig,
        notification: AuthSecurityEventNotificationDelivery,
    ): Set<String> {
        val rosterCode = config.responderRosterCode ?: return emptySet()
        val resolver = responderResolver ?: return emptySet()
        return resolver.resolve(
            tenantId = config.tenantId,
            rosterCode = rosterCode,
            escalationLevel = notification.escalationLevel,
            at = LocalDateTime.now(clock),
        )
    }

    /**
     * The configured rule can no longer address anybody — every named responder is gone, nobody is on call, or
     * the assignee snapshot the rule relies on is absent. The tenant chose in advance what should happen; none
     * of the three outcomes guesses at a substitute recipient.
     */
    private fun fallback(
        config: AuthSecurityEventNotificationRouteConfig,
        notification: AuthSecurityEventNotificationDelivery,
    ): AuthSecurityEventNotificationRoute = when (config.fallbackBehavior) {
        AuthSecurityEventNotificationRouteFallbackEnum.DEFAULT_ROUTE -> defaultPolicy.resolve(notification)
        AuthSecurityEventNotificationRouteFallbackEnum.TENANT_SECURITY_QUEUE ->
            AuthSecurityEventNotificationRoute(
                routeCode = DefaultAuthSecurityEventNotificationRoutePolicy.TENANT_SECURITY_QUEUE_ROUTE,
                destination = AuthSecurityEventNotificationDestinationEnum.TENANT_SECURITY_QUEUE,
                channels = setOf(AuthSecurityEventNotificationChannelEnum.EVENT_BUS),
                recipientUserIds = emptySet(),
            )
        // The dispatcher turns this into a DEAD row with an invalid-route code: an explicit, replayable
        // record that the tenant asked for no silent substitution.
        AuthSecurityEventNotificationRouteFallbackEnum.FAIL ->
            throw AuthSecurityEventException(ROUTE_UNRESOLVABLE)
    }

    private companion object {
        const val ROUTE_UNRESOLVABLE = "AUTH_SECURITY_EVENT_NOTIFICATION_ROUTE_INVALID"
    }
}
