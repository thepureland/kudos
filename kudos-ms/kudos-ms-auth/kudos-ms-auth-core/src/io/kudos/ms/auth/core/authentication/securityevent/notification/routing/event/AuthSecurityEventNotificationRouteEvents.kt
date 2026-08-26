package io.kudos.ms.auth.core.authentication.securityevent.notification.routing.event

/**
 * A tenant's notification routing configuration changed.
 *
 * Carries the tenant only: the payload is an invalidation signal, not a copy of the rule, so nothing about
 * responders or channels travels through the event bus or the cross-node cache notification.
 */
data class AuthSecurityEventNotificationRouteChanged(val tenantId: String)
