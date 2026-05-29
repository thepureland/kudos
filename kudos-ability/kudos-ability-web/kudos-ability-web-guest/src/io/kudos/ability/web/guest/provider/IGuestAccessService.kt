package io.kudos.ability.web.guest.provider

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

/**
 * Drives the per-request visitor lifecycle that [io.kudos.ability.web.guest.filter.GuestAccessFilter]
 * orchestrates.
 *
 * Lifecycle, per inbound request:
 *  1. [isEnabled] — fast short-circuit gate; nothing else runs when false.
 *  2. [isExclude] — composition of [IGuestAccessIgnore] beans; lets apps skip authed users,
 *     internal probes, etc.
 *  3. [fetchGuestToken] — read & decrypt the cookie. Returns null when no valid cookie is
 *     present (first request from this client / cookie tampered / cookie expired on the wire).
 *  4. If a token was found → [hash] populates the [GuestAccess.hash] and payload, then the store
 *     persists it.
 *  5. If no token → [genToken] sets a new cookie on the response, no store write happens this
 *     request (two-phase model: presence is recorded on the *second* visit only).
 *
 * The two-phase design intentionally drops one-shot script attacks that don't carry the issued
 * cookie back — they only allocate cookies, never end up in the visitor count.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IGuestAccessService {

    /** Mirrors `GuestProperties.enabled`; cached to avoid a property re-read per request. */
    fun isEnabled(): Boolean

    /**
     * Any [IGuestAccessIgnore] returns true → the request bypasses guest tracking entirely.
     * Composes via short-circuit OR.
     */
    fun isExclude(request: HttpServletRequest): Boolean

    /**
     * Read the visitor token cookie off the request. Returns:
     *  - null when no cookie / blank cookie / wrong-name cookie is present;
     *  - null when a cookie is present but fails AES decryption (tamper / wrong key) — the
     *    request is treated as first-visit and the filter mints a fresh cookie;
     *  - a populated [GuestAccess] with [GuestAccess.token] set when an existing visitor is back.
     *    Note the token field here holds the **decrypted** plaintext (the UUID), whereas the
     *    token set by [genToken] holds the **encrypted ciphertext** ready to ship as a cookie —
     *    asymmetric by design (matches soul's lifecycle semantics).
     */
    fun fetchGuestToken(request: HttpServletRequest): GuestAccess?

    /**
     * Populate [guestAccess.hash] + [guestAccess.payload] from request data. Called only on the
     * "returning visitor" branch — the filter then hands the populated [GuestAccess] to
     * [IGuestAccessStore.store].
     */
    fun hash(request: HttpServletRequest, guestAccess: GuestAccess)

    /**
     * Mint a new AES-encrypted token, set it as a `Set-Cookie` on the response, and return a
     * [GuestAccess] holding the same token. Called on the "first visit" branch; the store is NOT
     * written this request — the visitor only becomes "known" once they come back with the
     * cookie.
     */
    fun genToken(request: HttpServletRequest, response: HttpServletResponse): GuestAccess
}
