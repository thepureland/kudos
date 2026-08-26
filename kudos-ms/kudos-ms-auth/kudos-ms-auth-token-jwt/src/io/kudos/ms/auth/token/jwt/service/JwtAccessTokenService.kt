package io.kudos.ms.auth.token.jwt.service

import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.common.authz.api.IPermissionVersionApi
import io.kudos.ms.auth.common.authz.vo.SubjectRef
import io.kudos.ms.auth.core.authentication.session.service.iservice.IAuthenticationSessionService
import io.kudos.ms.auth.token.jwt.JwtTokenProperties
import io.kudos.ms.auth.token.jwt.model.AccessTokenException
import io.kudos.ms.auth.token.jwt.model.IJwtAccessTokenService
import io.kudos.ms.auth.token.jwt.model.IssuedAccessToken
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwsHeader
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/** JWT access tokens whose signature, permission version and logical session are all verified. */
open class JwtAccessTokenService(
    private val encoder: JwtEncoder,
    private val decoder: JwtDecoder,
    private val sessionService: IAuthenticationSessionService,
    private val permissionVersionApi: IPermissionVersionApi,
    private val properties: JwtTokenProperties,
) : IJwtAccessTokenService {

    override fun issue(session: AuthenticationSession): IssuedAccessToken {
        require(session.isActive()) { "Cannot issue an access token for an inactive session" }
        val now = Instant.now()
        val expiresAt = minOf(
            now.plus(properties.accessTtlSeconds.coerceIn(1, MAX_ACCESS_TTL_SECONDS), ChronoUnit.SECONDS),
            session.absoluteExpiresAt,
        )
        val permissionVersion = permissionVersionApi.currentVersion(SubjectRef.ofUser(session.userId))
        val claims = JwtClaimsSet.builder()
            .issuer(properties.issuer)
            .audience(listOf(properties.audience))
            .subject(session.userId)
            .id(UUID.randomUUID().toString())
            .issuedAt(now)
            .notBefore(now)
            .expiresAt(expiresAt)
            .claim(CLAIM_TOKEN_USE, ACCESS_TOKEN_USE)
            .claim(CLAIM_SESSION_ID, session.id)
            .claim(CLAIM_TENANT_ID, session.tenantId)
            .claim(CLAIM_PERMISSION_VERSION, permissionVersion)
            .claim(CLAIM_AUTH_TIME, session.authTime.epochSecond)
            .claim(CLAIM_AMR, session.amr.toList().sorted())
            .claim(CLAIM_ACR, session.acr)
            .apply { session.clientId?.let { claim(CLAIM_CLIENT_ID, it) } }
            .build()
        val algorithm = SignatureAlgorithm.from(properties.signingAlgorithm)
            ?: throw IllegalArgumentException("Unsupported JWT signing algorithm: ${properties.signingAlgorithm}")
        val header = JwsHeader.with(algorithm)
            .type(ACCESS_TOKEN_TYPE)
            .apply { properties.keyId?.trim()?.takeIf(String::isNotEmpty)?.let(::keyId) }
            .build()
        val token = encoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
        return IssuedAccessToken(token, expiresAt)
    }

    override fun verify(token: String): AuthenticationSession {
        try {
            val jwt = decoder.decode(token)
            val now = Instant.now()
            if (jwt.issuer?.toString() != properties.issuer || properties.audience !in jwt.audience.orEmpty() ||
                jwt.expiresAt?.isAfter(now) != true || jwt.notBefore?.isAfter(now) == true ||
                jwt.getClaimAsString(CLAIM_TOKEN_USE) != ACCESS_TOKEN_USE
            ) invalid()
            val userId = jwt.subject?.takeIf(String::isNotBlank) ?: invalid()
            val tenantId = jwt.getClaimAsString(CLAIM_TENANT_ID)?.takeIf(String::isNotBlank) ?: invalid()
            val sessionId = jwt.getClaimAsString(CLAIM_SESSION_ID)?.takeIf(String::isNotBlank) ?: invalid()
            val permissionVersion = jwt.getClaimAsString(CLAIM_PERMISSION_VERSION) ?: invalid()
            if (!permissionVersionApi.isCurrent(SubjectRef.ofUser(userId), permissionVersion)) invalid()
            return sessionService.touch(sessionId)
                ?.takeIf { it.isActive() && it.userId == userId && it.tenantId == tenantId }
                ?: invalid()
        } catch (_: AccessTokenException) {
            throw AccessTokenException()
        } catch (_: Exception) {
            throw AccessTokenException()
        }
    }

    private fun invalid(): Nothing = throw AccessTokenException()

    private companion object {
        const val MAX_ACCESS_TTL_SECONDS = 3_600L
        // Keep Spring Security's secure default typ validation interoperable; token_use distinguishes access JWTs.
        const val ACCESS_TOKEN_TYPE = "JWT"
        const val ACCESS_TOKEN_USE = "access"
        const val CLAIM_TOKEN_USE = "token_use"
        const val CLAIM_SESSION_ID = "sid"
        const val CLAIM_TENANT_ID = "tenant_id"
        const val CLAIM_PERMISSION_VERSION = "pv"
        const val CLAIM_AUTH_TIME = "auth_time"
        const val CLAIM_AMR = "amr"
        const val CLAIM_ACR = "acr"
        const val CLAIM_CLIENT_ID = "client_id"
    }
}
