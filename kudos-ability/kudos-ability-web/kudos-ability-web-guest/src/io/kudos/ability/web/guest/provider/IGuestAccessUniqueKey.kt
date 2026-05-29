package io.kudos.ability.web.guest.provider

import jakarta.servlet.http.HttpServletRequest

/**
 * Strategy for deriving a stable per-visitor key from the inbound request.
 *
 * The default ([GuestAccessUniqueKey]) hashes `User-Agent + remoteIp` with the cookie cipher key
 * as salt. Apps that want stricter or looser fingerprints — e.g. ignore UA so visitors on
 * desktop+mobile count once, or add an `X-Device-Id` from a custom auth flow — declare their own
 * bean implementing this contract.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
fun interface IGuestAccessUniqueKey {

    /** Compute the fingerprint for this request. Must be stable: same input → same output. */
    fun gen(request: HttpServletRequest): String
}
