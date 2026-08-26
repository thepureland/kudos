package io.kudos.ms.auth.core.authentication.securityevent.oncall.event

/**
 * A tenant's on-call rotations changed.
 *
 * Carries the tenant only: the payload is an invalidation signal, so no responder identity travels through
 * the event bus or the cross-node cache notification.
 */
data class AuthSecurityEventOnCallRosterChanged(val tenantId: String)
