package io.kudos.ms.auth.core.authentication.securityevent.oncall.spi

import java.time.LocalDateTime

/**
 * Resolves who should answer for a tenant right now.
 *
 * Protocol-neutral on purpose: the contract is "a roster code and an escalation level in, local user ids out".
 * A deployment that keeps its rotation in PagerDuty, an HR system or a duty spreadsheet replaces this bean,
 * and nothing vendor-specific enters core.
 *
 * **Failure semantics.** Returning an empty set means "nobody is on call", which the route policy answers with
 * the tenant's configured fallback. Throwing means "the rotation could not be read", which the dispatcher
 * answers with a retry: a temporary directory outage must not be recorded as a routing decision.
 */
fun interface IAuthSecurityEventResponderResolver {

    /**
     * @param tenantId the tenant the incident belongs to
     * @param rosterCode the rotation named by the tenant's route rule
     * @param escalationLevel the outbox row's escalation level; a level of N reaches tiers 1..N
     * @param at the instant to resolve for, in UTC
     * @return the responders on call, empty when nobody is
     */
    fun resolve(
        tenantId: String,
        rosterCode: String,
        escalationLevel: Int,
        at: LocalDateTime,
    ): Set<String>
}
