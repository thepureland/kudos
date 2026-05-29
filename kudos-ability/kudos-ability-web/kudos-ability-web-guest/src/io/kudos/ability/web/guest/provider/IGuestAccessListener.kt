package io.kudos.ability.web.guest.provider

/**
 * Event hook fired by the store whenever a visitor transitions to "active".
 *
 * "Active" means either:
 *  - a brand-new visitor record was written (the visitor returned with the cookie minted on a
 *    previous request), or
 *  - an existing record expired in the store and is now being re-created (long-absent visitor
 *    coming back).
 *
 * **Not** fired on a simple TTL roll of an already-live record — the listener is meant for
 * lifecycle signals (analytics, presence notifications), not for hot-path traffic.
 *
 * Optional bean: if no listener is registered the store silently skips the call.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
fun interface IGuestAccessListener {

    /** Invoked when a visitor record transitions to active. */
    fun active(guestAccess: GuestAccess)
}
