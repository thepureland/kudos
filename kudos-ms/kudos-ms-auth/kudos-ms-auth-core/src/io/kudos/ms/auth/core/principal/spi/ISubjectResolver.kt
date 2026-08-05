package io.kudos.ms.auth.core.principal.spi

import io.kudos.ms.auth.common.authz.vo.SubjectRef


/**
 * Works out who is making the current request.
 *
 * **Why this is an SPI and not a constant.** The enforcement points pass only the permission code —
 * deliberately, so no caller can have a check performed as somebody else — which means the subject
 * has to come from ambient request state. For a human that is the login session, and kudos knows how
 * to read it. For a machine caller it is something kudos cannot know: an mTLS certificate subject,
 * an API key header, a signed service token, a cloud workload identity. Each deployment has one, and
 * none of them belong in this module.
 *
 * Without this hook, machine access to a kudos service has nowhere to go but around the
 * authorization system — a shared human account, a bypass in a filter, an allow-list in code. Those
 * are not grantable, revocable or auditable, which defeats the point of having the system.
 *
 * **Contract.** Return null for "not my kind of caller"; resolvers are tried in [order] and the
 * first non-null wins. A resolver must **authenticate**, not merely read: returning a subject
 * because a header said so, with nothing verifying it, hands anyone the ability to name themselves.
 * The built-in login-state resolver is last, so a machine identity is recognised as such rather
 * than being mistaken for whatever session happens to be lying around.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface ISubjectResolver {

    /**
     * The subject of the current request, or null when this resolver does not recognise the caller.
     */
    fun resolve(): SubjectRef?

    /**
     * Lower runs first. The built-in login-state resolver sits at [LOGIN_STATE_ORDER]; a machine
     * resolver should come before it.
     */
    fun order(): Int = 0

    companion object {
        /** Where the built-in human-session resolver sits. */
        const val LOGIN_STATE_ORDER = 1000
    }
}
