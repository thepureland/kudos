package io.kudos.ability.security.jwt.support

import io.kudos.ability.security.jwt.exception.JwtExpiredException
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertTrue

/**
 * Unit tests for [JwtExpValidator].
 *
 * Uses a fixed-instant [Clock] rather than `Clock.systemUTC()` so the "expired by N seconds"
 * boundaries are deterministic. The fixture instant is arbitrary (year 2026); what matters is
 * that the `expiresAt` arguments are computed *relative to that instant*.
 *
 * @author AI: Claude
 * @since 1.0.0
 */
internal class JwtExpValidatorTest {

    private val fixedNow = Instant.parse("2026-06-15T12:00:00Z")
    private val fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC)

    @Test
    fun validate_missingExp_passesThrough() {
        // A token without an exp claim is treated as "no expiration to compare against" — the
        // validator returns success rather than rejecting. Matches Spring's own JwtTimestampValidator.
        val jwt = newJwt(expiresAt = null)
        val validator = JwtExpValidator(clock = fixedClock)
        assertTrue(validator.validate(jwt).errors.isEmpty())
    }

    @Test
    fun validate_futureExp_passesThrough() {
        val jwt = newJwt(expiresAt = fixedNow.plus(Duration.ofMinutes(5)))
        val validator = JwtExpValidator(clock = fixedClock)
        assertTrue(validator.validate(jwt).errors.isEmpty())
    }

    @Test
    fun validate_pastExp_throwsJwtExpiredException() {
        val jwt = newJwt(expiresAt = fixedNow.minus(Duration.ofMinutes(5)))
        val validator = JwtExpValidator(clock = fixedClock)
        val ex = assertFails { validator.validate(jwt) }
        assertTrue(
            ex is JwtExpiredException,
            "expired tokens must throw JwtExpiredException so the existing BadJwtException-to-401 mapping fires; got ${ex::class}",
        )
    }

    @Test
    fun validate_recentlyExpired_withinClockSkew_passesThrough() {
        // A token that expired 10s ago must NOT be rejected when clockSkew is 30s — this is the
        // whole point of the clockSkew argument, accommodating mild client/server time drift.
        val jwt = newJwt(expiresAt = fixedNow.minus(Duration.ofSeconds(10)))
        val validator = JwtExpValidator(clockSkew = Duration.ofSeconds(30), clock = fixedClock)
        assertTrue(validator.validate(jwt).errors.isEmpty())
    }

    @Test
    fun validate_recentlyExpired_beyondClockSkew_throws() {
        // 60s past expiry with a 30s skew tolerance → still expired.
        val jwt = newJwt(expiresAt = fixedNow.minus(Duration.ofSeconds(60)))
        val validator = JwtExpValidator(clockSkew = Duration.ofSeconds(30), clock = fixedClock)
        assertFails { validator.validate(jwt) }
    }

    @Test
    fun validate_zeroClockSkew_isDefault_andEqualToExplicit() {
        // Sanity: the convenience no-arg constructor must behave identical to passing Duration.ZERO.
        val jwt = newJwt(expiresAt = fixedNow.minus(Duration.ofMillis(1)))
        val defaultCtor = JwtExpValidator(clock = fixedClock)
        val zeroSkew = JwtExpValidator(clockSkew = Duration.ZERO, clock = fixedClock)
        assertFails { defaultCtor.validate(jwt) }
        assertFails { zeroSkew.validate(jwt) }
    }

    private fun newJwt(expiresAt: Instant?): Jwt {
        val builder = Jwt.withTokenValue("test-token-value")
            .header("alg", "RS256")
            .subject("test-subject")
        if (expiresAt != null) {
            builder.expiresAt(expiresAt)
        }
        return builder.build()
    }
}
