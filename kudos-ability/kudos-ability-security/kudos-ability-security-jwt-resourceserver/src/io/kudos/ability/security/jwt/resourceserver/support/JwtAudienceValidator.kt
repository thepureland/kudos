package io.kudos.ability.security.jwt.resourceserver.support

import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Validator that requires the JWT's `aud` (audience) claim to contain a configured value.
 *
 * Why it exists: the decoder's signature check only proves the token was minted with the shared
 * keystore — when several services share that keystore, a token minted for service A verifies on
 * service B as well. Pinning the expected audience per service closes that lateral-replay hole
 * (together with the issuer check wired by
 * [io.kudos.ability.security.jwt.resourceserver.init.JwtResourceServerAutoConfiguration]).
 *
 * Semantics follow RFC 7519 section 4.1.3: `aud` is a list of recipients; validation succeeds when
 * the configured value is one of them. A missing `aud` claim fails — "no audience" cannot satisfy
 * "must be intended for me". Failure is reported as a standard
 * [OAuth2TokenValidatorResult.failure] with `invalid_token`, which Spring Security's resource
 * server translates to a 401 — same path Spring's own claim validators take, no custom exception.
 *
 * The error description is intentionally static (no interpolation of the configured value): it
 * ends up in the `WWW-Authenticate` response header, where unexpected characters from config
 * would be rejected by the header writer.
 *
 * @author AI: Claude
 * @since 1.0.0
 */
class JwtAudienceValidator(
    private val expectedAudience: String,
) : OAuth2TokenValidator<Jwt> {

    init {
        require(expectedAudience.isNotBlank()) { "expectedAudience must not be blank" }
    }

    override fun validate(token: Jwt): OAuth2TokenValidatorResult {
        val audiences = token.audience ?: emptyList()
        if (expectedAudience in audiences) {
            return OAuth2TokenValidatorResult.success()
        }
        return OAuth2TokenValidatorResult.failure(
            OAuth2Error(
                OAuth2ErrorCodes.INVALID_TOKEN,
                "The aud claim does not contain the required audience",
                "https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.3",
            ),
        )
    }
}
