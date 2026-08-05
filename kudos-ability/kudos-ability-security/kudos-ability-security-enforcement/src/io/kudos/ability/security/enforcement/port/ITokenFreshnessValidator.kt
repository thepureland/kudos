package io.kudos.ability.security.enforcement.port


/**
 * Whether the credential the caller presented still describes their current permissions.
 *
 * **The gap this closes.** A signed token is believed because of its signature, not because anything
 * checked it against the current state. Take a permission away and the token minted five minutes ago
 * keeps exercising it until it expires. The authorization model can be perfectly correct and the
 * request still gets through, because nothing on the request path ever asks whether the credential is
 * still describing reality.
 *
 * The decision point cannot answer this: it is asked "may this subject do X", and the subject it is
 * given came *from* the token. Whether that token is stale is a question about the credential, which
 * is why it is a separate port with its own gate.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface ITokenFreshnessValidator {

    /**
     * Assesses the current request's credential.
     *
     * Implementations resolve both the subject and the presented version themselves — the
     * enforcement points pass nothing, so no caller can have the check performed against somebody
     * else's credential.
     */
    fun assess(): Freshness

    /**
     * The three genuinely different answers.
     *
     * [UNKNOWN] exists so a deployment can adopt this without a flag day. It is *not* a synonym for
     * fresh: it means the credential carries no version at all, which is the normal state of every
     * token minted before the mechanism existed. Treating it as fresh forever would mean the check
     * never actually starts working; treating it as stale immediately would log out everyone holding
     * an old token the moment the feature is switched on. So the filter decides, from configuration,
     * which of those two it is — and the default is the migration-friendly one.
     */
    enum class Freshness {
        /** The presented version matches the subject's current one. */
        FRESH,

        /** The subject holds a version, and it is not this one — the credential predates a change. */
        STALE,

        /** No subject, or no version presented. The policy for this is the deployment's to choose. */
        UNKNOWN,
    }
}
