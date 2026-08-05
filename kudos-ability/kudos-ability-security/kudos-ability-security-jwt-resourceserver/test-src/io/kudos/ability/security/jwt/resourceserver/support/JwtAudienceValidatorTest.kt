package io.kudos.ability.security.jwt.resourceserver.support

import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [JwtAudienceValidator].
 *
 * @author AI: Claude
 * @since 1.0.0
 */
internal class JwtAudienceValidatorTest {

    private val validator = JwtAudienceValidator("svc-a")

    @Test
    fun audienceContainsExpectedValue_passes() {
        val jwt = newJwt(audience = listOf("svc-a"))
        assertFalse(validator.validate(jwt).hasErrors())
    }

    @Test
    fun multipleAudiences_oneMatches_passes() {
        // RFC 7519: aud is a list of recipients; the token is valid for each of them.
        val jwt = newJwt(audience = listOf("svc-b", "svc-a", "svc-c"))
        assertFalse(validator.validate(jwt).hasErrors())
    }

    @Test
    fun wrongAudience_isRejected_withInvalidTokenError() {
        // The lateral-replay case: a token minted for svc-b presented to svc-a.
        val jwt = newJwt(audience = listOf("svc-b"))
        val result = validator.validate(jwt)
        assertTrue(result.hasErrors(), "token for another audience must be rejected")
        assertEquals(
            OAuth2ErrorCodes.INVALID_TOKEN, result.errors.single().errorCode,
            "failure must use the standard invalid_token error code so Spring maps it to 401",
        )
    }

    @Test
    fun missingAudienceClaim_isRejected() {
        // "no audience" cannot satisfy "must be intended for me" — a token without aud must not
        // bypass the check, otherwise the validator is trivially defeated by omitting the claim.
        val jwt = newJwt(audience = null)
        assertTrue(validator.validate(jwt).hasErrors())
    }

    @Test
    fun caseDiffers_isRejected() {
        // Exact match only — audience identifiers are opaque strings, not case-insensitive hosts.
        val jwt = newJwt(audience = listOf("SVC-A"))
        assertTrue(validator.validate(jwt).hasErrors())
    }

    @Test
    fun blankExpectedAudience_throwsAtConstruction() {
        // The autoconfig only builds the validator for non-blank config; the constructor guard
        // keeps direct users from accidentally creating a validator that matches nothing.
        assertFailsWith<IllegalArgumentException> { JwtAudienceValidator("  ") }
    }

    private fun newJwt(audience: List<String>?): Jwt {
        val builder = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .subject("test-sub")
        if (audience != null) {
            builder.audience(audience)
        }
        return builder.build()
    }
}
