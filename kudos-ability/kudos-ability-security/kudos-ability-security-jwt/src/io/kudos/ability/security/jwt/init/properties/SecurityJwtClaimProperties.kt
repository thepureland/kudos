package io.kudos.ability.security.jwt.init.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Instant
import java.util.UUID

/**
 * Default-claim configuration for tokens minted by [io.kudos.ability.security.jwt.support.JwtParametersTool].
 *
 * Each field is a "claim default" — `JwtParametersTool.createDefault(...)` reads these getters at
 * token-mint time and stuffs them into the `JwtClaimsSet.Builder`. Apps can override per call by
 * passing a custom claims map.
 *
 * The "magic string" parsing rules below mirror soul's surface — `"now()"` / `"uuid()"` look
 * tacky but matter for migration: existing soul deployments have these strings sitting in their
 * yml today. Keeping the same parsing lets them swap dependencies without yml edits.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "kudos.ability.security.jwt.claims")
class SecurityJwtClaimProperties {

    /** Issuer; appears as the `iss` claim. Typically the service name. */
    var iss: String? = null

    /** Subject default; per-token overrides via [JwtParametersTool.createDefault(String)]. */
    var sub: String? = null

    /** Audience; appears as the `aud` claim. Usually the relying-party identifier. */
    var aud: String? = null

    /** Time-to-live in seconds. `getExpireAt()` resolves to `now() + exp` at mint time. */
    var exp: Int? = null

    /**
     * "not-before" raw setting. Two parse rules: `"now()"` → [Instant.now]; numeric → offset
     * seconds from now. Any other string → null (the claim is omitted).
     */
    var nbf: String? = null

    /**
     * "issued-at" raw setting. Currently only `"now()"` is recognized; other values yield null.
     * Soul intentionally limits this to `now()` because business code that explicitly sets `iat`
     * should usually pass it per-token rather than via yml defaults.
     */
    var iat: String? = null

    /**
     * "JWT id" raw setting. `"uuid()"` → fresh UUID-without-hyphens per call. Any other value
     * resolves to empty string (which then gets omitted from the claim set by
     * `JwtClaimsSet.Builder.id`).
     */
    var jti: String? = null

    /** Resolves [exp] to an absolute [Instant] at mint time. Null when unconfigured. */
    fun resolveExpireAt(): Instant? = exp?.let { Instant.now().plusSeconds(it.toLong()) }

    /** Resolves [nbf] per the rules documented on the field. Null when unconfigured / unrecognized. */
    fun resolveNotBefore(): Instant? {
        val raw = nbf ?: return null
        if (raw == NOW) return Instant.now()
        val offset = raw.toIntOrNull() ?: return null
        return if (offset > 0) Instant.now().plusSeconds(offset.toLong()) else null
    }

    /** Resolves [iat]; only `"now()"` is recognized. */
    fun resolveIssuedAt(): Instant? = if (iat == NOW) Instant.now() else null

    /** Resolves [jti]; `"uuid()"` → fresh UUID (no hyphens); otherwise empty string. */
    fun resolveJwtId(): String =
        if (jti == UUID_FN) UUID.randomUUID().toString().replace("-", "") else ""

    companion object {
        private const val NOW = "now()"
        private const val UUID_FN = "uuid()"
    }
}
