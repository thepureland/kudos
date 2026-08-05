package io.kudos.ms.auth.common.authz.api

import io.kudos.ms.auth.common.authz.vo.SubjectRef


/**
 * Makes a stateless token revocable.
 *
 * **The problem.** A JWT is believed because it is signed, not because it is checked against
 * anything. Take a permission away at 10:00 and the token minted at 09:55 keeps exercising it until
 * it expires. Shortening the TTL trades that window against re-issue traffic and never closes it.
 *
 * **The fix.** Mint the token with the subject's current permission version; have the enforcement
 * point compare it against the live one per request. A mismatch means the token predates a change
 * and must be re-issued. The comparison is a cache hit, so this costs the request nothing — and it
 * is precisely *not* a session store: nothing is written per login, and no state accumulates.
 *
 * **What makes the version change.** Two independent things, and it matters that both do:
 *  - the subject's resolved permissions changed — derived, so it needs no bookkeeping and, usefully,
 *    does *not* fire when a role is rebound to the same set of codes;
 *  - an administrator forced it ([revokeAllTokens]) — for a leaked credential or a departure, where
 *    the permissions may be entirely unchanged and every token must still die.
 *
 * **What this is not.** It does not make the gateway's coarse `roles` claim trustworthy for
 * fine-grained decisions; that remains a server-side question ([IAuthzDecisionApi]). It closes the
 * window on the coarse layer, which is the layer that has one.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IPermissionVersionApi {

    /**
     * The subject's current permission version — an opaque, short, comparable string.
     *
     * Opaque on purpose: it is a fingerprint, not a counter, and callers must only ever test it for
     * equality. Treating it as ordered would be wrong — a permission set that returns to an earlier
     * state legitimately returns to its earlier fingerprint.
     *
     * Stamp this into the token at mint time.
     *
     * @param subject the principal
     * @return the version to embed in a freshly minted token
     */
    fun currentVersion(subject: SubjectRef): String

    /**
     * Whether a token bearing [version] is still current for [subject].
     *
     * On every request, so it must resolve from cache. A null or blank [version] returns false: a
     * token minted before this mechanism existed cannot be shown to be current, and treating
     * "unknown" as "fine" is how a revocation check quietly stops checking. Deployments migrating
     * an existing token population should run the comparison in shadow first, exactly as with URL
     * enforcement.
     *
     * @param subject the principal
     * @param version the version carried by the token
     * @return true when the token is current
     */
    fun isCurrent(subject: SubjectRef, version: String?): Boolean

    /**
     * Forces every token the principal holds to be stale, immediately.
     *
     * The administrative half — for cases where the permissions did not change at all and the
     * tokens must still die. Monotonic: the epoch only ever increases, so a later permission change
     * that happens to reproduce an earlier state can never resurrect a token this call killed.
     *
     * @param principalId the principal
     * @param reason why (credential leak, departure, forced re-authentication) — stored for audit
     * @return the new epoch
     */
    fun revokeAllTokens(principalId: String, reason: String?): Long
}
