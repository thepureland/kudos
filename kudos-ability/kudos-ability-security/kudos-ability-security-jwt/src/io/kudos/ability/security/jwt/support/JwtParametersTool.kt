package io.kudos.ability.security.jwt.support

import io.kudos.ability.security.jwt.init.properties.SecurityJwtClaimProperties
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoderParameters

/**
 * Convenience builder for [JwtEncoderParameters].
 *
 * Reads its claim defaults from [SecurityJwtClaimProperties] each time `createDefault*` is called
 * — the resolution is intentionally dynamic per call (each call gets fresh `now()` / `uuid()`
 * values) so a long-lived `JwtParametersTool` bean doesn't bake in `iat`/`jti` at boot time.
 *
 * Header is fixed to [SignatureAlgorithm.RS256]. Apps that need ES256 / HS256 / etc. should build
 * their own [JwtEncoderParameters] instead of going through this tool — the keystore-backed
 * auto-config in this module is RSA-only.
 *
 * Ported from soul's `JwtParametersTool` with two cleanups:
 *  - Dropped the two `@Deprecated` overloads taking `Date expireAt` — `JwtClaimsSet.Builder`
 *    already expects `Instant`; mixing `Date` was a leftover from earlier soul iterations.
 *  - Replaced the property-setter Spring wiring with constructor injection — modern bean style,
 *    and makes [SecurityJwtClaimProperties] a hard dependency at construction (no NPE risk
 *    later).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class JwtParametersTool(
    private val claimProperties: SecurityJwtClaimProperties,
) {

    /** Build parameters using only the yml-configured defaults. */
    fun createDefault(): JwtEncoderParameters =
        JwtEncoderParameters.from(rs256Header(), buildDefaultClaims().build())

    /** Build parameters with an explicit `sub` override; other claims come from yml defaults. */
    fun createDefault(subject: String): JwtEncoderParameters {
        require(subject.isNotBlank()) { "subject must not be blank" }
        return JwtEncoderParameters.from(rs256Header(), buildDefaultClaims().subject(subject).build())
    }

    /**
     * Build parameters with custom claims merged into the defaults. The custom claims are added
     * via [JwtClaimsSet.Builder.claim], so callers passing a key that already has a default get
     * the default overridden. Entries with a null value are skipped — [JwtClaimsSet.Builder.claim]
     * rejects null values since Spring Security 7.1.
     */
    fun createDefault(customClaims: Map<String, Any?>): JwtEncoderParameters {
        val builder = buildDefaultClaims()
        customClaims.forEach { (name, value) -> value?.let { builder.claim(name, it) } }
        return JwtEncoderParameters.from(rs256Header(), builder.build())
    }

    /** Combines [createDefault] + [createDefault(Map)] in one call. */
    fun createDefault(subject: String, customClaims: Map<String, Any?>): JwtEncoderParameters {
        require(subject.isNotBlank()) { "subject must not be blank" }
        val builder = buildDefaultClaims().subject(subject)
        customClaims.forEach { (name, value) -> value?.let { builder.claim(name, it) } }
        return JwtEncoderParameters.from(rs256Header(), builder.build())
    }

    private fun rs256Header(): JwsHeader = JwsHeader.with(SignatureAlgorithm.RS256).build()

    private fun buildDefaultClaims(): JwtClaimsSet.Builder = JwtClaimsSet.builder().apply {
        id(claimProperties.resolveJwtId())
        claimProperties.iss?.let { issuer(it) }
        claimProperties.sub?.let { subject(it) }
        claimProperties.aud?.let { audience(listOf(it)) }
        claimProperties.resolveExpireAt()?.let { expiresAt(it) }
        claimProperties.resolveIssuedAt()?.let { issuedAt(it) }
        claimProperties.resolveNotBefore()?.let { notBefore(it) }
    }
}
