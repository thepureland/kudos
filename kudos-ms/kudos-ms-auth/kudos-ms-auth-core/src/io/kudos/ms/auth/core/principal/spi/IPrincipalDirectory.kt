package io.kudos.ms.auth.core.principal.spi


/**
 * The facts the authorization model needs about a principal, whatever kind of thing it is.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class PrincipalFacts(

    /** The principal's id. */
    val principalId: String,

    /** Subject kind: USER / SERVICE / API_KEY / whatever a deployment registers. */
    val principalType: String,

    /** Tenant, the hard boundary of the whole model. Null only for platform-level principals. */
    val tenantId: String?,

    /** Whether the principal is usable at all; a disabled one may hold no new grants. */
    val active: Boolean = true,

    /** For messages and audit lines. */
    val displayName: String? = null,
)


/**
 * Resolves a principal to [PrincipalFacts] — the SPI that makes a non-human subject a first-class
 * one.
 *
 * **Why this has to exist.** The policy layer must answer two questions about the receiver of every
 * grant: does it exist, and which tenant is it in. Those were answered by reading `user_account`
 * directly, which quietly made "principal" mean "human account" — a service account or an API key
 * could be *modelled* (the `principal_type` column has always been there) but could never actually
 * be granted anything, because the existence check would refuse it. Machine access then gets built
 * some other way: a shared human account, a bypass in a filter, an allow-list in code. None of those
 * are grantable, revocable or auditable, which is the whole point of having an authorization system.
 *
 * **Contract.** A provider claims one [principalType] and answers only for that kind. Returning null
 * means "not mine, or not found" — the policy layer refuses a principal no provider claims, because
 * granting to an unknown receiver is exactly the mistake that should be loud.
 *
 * **Invariant this rests on.** Principal ids are unique *across kinds*, not merely within one. That
 * is what lets the decision path key on the id alone and lets a role be granted to a service account
 * with no ambiguity. Ids here are opaque and generated (UUIDs for kudos' own tables), so this costs
 * nothing to honour — but a deployment inventing sequential per-kind ids would break it, and would
 * break it silently, in the direction of one principal inheriting another's permissions.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IPrincipalDirectory {

    /** The subject kind this provider answers for, e.g. `USER`. */
    fun principalType(): String

    /**
     * @param principalId the principal
     * @return its facts, or null when this provider does not know it
     */
    fun find(principalId: String): PrincipalFacts?
}
