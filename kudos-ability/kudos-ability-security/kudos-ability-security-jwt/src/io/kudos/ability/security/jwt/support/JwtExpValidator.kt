package io.kudos.ability.security.jwt.support

import io.kudos.ability.security.jwt.exception.JwtExpiredException
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Single-purpose JWT validator that checks only the `exp` claim.
 *
 * Why a custom validator instead of Spring's `JwtTimestampValidator`:
 *  - The default Nimbus claims-set verifier (which `NimbusJwtDecoder` installs) also enforces
 *    `nbf` and other standard timestamps. In some kudos-friendly token shapes (very short-lived,
 *    no `nbf`), the extra checks are noise. This validator is what gets registered when the app
 *    wants "expired = reject, everything else = allow".
 *  - Throws [JwtExpiredException] (a `BadJwtException`) rather than returning a failure
 *    `OAuth2TokenValidatorResult`. Soul's port did this; the kudos port keeps it for behaviour
 *    parity. Note that `BadJwtException` already maps to a 401 in Spring Security's exception
 *    translator, so this is safe to propagate.
 *
 * Tokens without an `exp` claim pass through as success — this is the conservative interpretation
 * of "we don't have an expiry to compare against", matching Spring's own
 * `JwtTimestampValidator` behaviour for missing `exp`.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class JwtExpValidator @JvmOverloads constructor(
    private val clockSkew: Duration = Duration.ZERO,
    private val clock: Clock = Clock.systemUTC(),
) : OAuth2TokenValidator<Jwt> {

    override fun validate(token: Jwt): OAuth2TokenValidatorResult {
        val expiry = token.expiresAt ?: return OAuth2TokenValidatorResult.success()
        if (Instant.now(clock).minus(clockSkew).isAfter(expiry)) {
            throw JwtExpiredException("exp is invalid, $expiry")
        }
        return OAuth2TokenValidatorResult.success()
    }
}
