package io.kudos.ms.auth.token.jwt

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.proc.SecurityContext
import io.kudos.ms.auth.common.authentication.vo.AuthenticationContext
import io.kudos.ms.auth.common.authz.api.IPermissionVersionApi
import io.kudos.ms.auth.common.authz.vo.SubjectRef
import io.kudos.ms.auth.core.authentication.session.model.AuthenticationSessionIssueCommand
import io.kudos.ms.auth.core.authentication.session.service.impl.AuthenticationSessionService
import io.kudos.ms.auth.core.authentication.session.store.InMemoryAuthenticationSessionStore
import io.kudos.ms.auth.token.jwt.model.AccessTokenException
import io.kudos.ms.auth.token.jwt.service.JwtAccessTokenService
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class JwtAccessTokenServiceTest {

    private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val publicKey = keyPair.public as RSAPublicKey
    private val privateKey = keyPair.private as RSAPrivateKey
    private val encoder = NimbusJwtEncoder(
        ImmutableJWKSet<SecurityContext>(
            JWKSet(RSAKey.Builder(publicKey).privateKey(privateKey).keyID("test-key").build())
        )
    )
    private val decoder = NimbusJwtDecoder.withPublicKey(publicKey).build()
    private val sessionService = AuthenticationSessionService(InMemoryAuthenticationSessionStore())
    private val versions = TestPermissionVersionApi()
    private val properties = JwtTokenProperties().apply {
        enabled = true
        // RFC 7519 also permits a plain StringOrURI value; deployments need not invent a URL.
        issuer = "party-games"
        audience = "kudos-api-test"
    }
    private val service = JwtAccessTokenService(
        encoder,
        decoder,
        sessionService,
        versions,
        properties,
    )

    private fun session() = sessionService.issue(
        AuthenticationSessionIssueCommand(
            context = AuthenticationContext(
                userId = "u-1",
                tenantId = "t-1",
                authTime = Instant.now(),
                amr = setOf("password", "totp"),
                acr = "urn:kudos:acr:mfa",
            ),
            clientId = "mobile",
        )
    )

    @Test
    fun issueAndVerify_bindsTokenToSessionTenantAndPermissionVersion() {
        val session = session()

        val issued = service.issue(session)
        val jwt = decoder.decode(issued.token)
        val verified = service.verify(issued.token)

        assertEquals(session.id, verified.id)
        assertEquals(session.id, jwt.getClaimAsString("sid"))
        assertEquals("t-1", jwt.getClaimAsString("tenant_id"))
        assertEquals("7:fingerprint", jwt.getClaimAsString("pv"))
        assertEquals("access", jwt.getClaimAsString("token_use"))
        assertEquals("party-games", jwt.getClaimAsString("iss"))
        assertTrue("kudos-api-test" in jwt.audience.orEmpty())
    }

    @Test
    fun verify_rejectsStalePermissionVersion() {
        val issued = service.issue(session())
        versions.version = "8:changed"

        assertFailsWith<AccessTokenException> { service.verify(issued.token) }
    }

    @Test
    fun verify_rejectsRevokedLogicalSession() {
        val session = session()
        val issued = service.issue(session)
        sessionService.revokeForUser(session.id, session.tenantId, session.userId, "ADMIN_REVOKE")

        assertFailsWith<AccessTokenException> { service.verify(issued.token) }
    }

    private class TestPermissionVersionApi : IPermissionVersionApi {
        var version = "7:fingerprint"
        override fun currentVersion(subject: SubjectRef): String = version
        override fun isCurrent(subject: SubjectRef, version: String?): Boolean = this.version == version
        override fun revokeAllTokens(principalId: String, reason: String?): Long = 0
    }
}
