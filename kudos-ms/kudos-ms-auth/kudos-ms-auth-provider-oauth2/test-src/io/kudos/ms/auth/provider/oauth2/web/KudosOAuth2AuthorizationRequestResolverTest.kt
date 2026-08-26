package io.kudos.ms.auth.provider.oauth2.web

import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionStatusEnum
import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction
import io.kudos.ms.auth.core.authentication.service.iservice.IAuthenticationTransactionService
import io.kudos.ms.auth.provider.oauth2.state.InMemoryExternalLoginStateStore
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class KudosOAuth2AuthorizationRequestResolverTest {
    private val registration = ClientRegistration.withRegistrationId("provider-1")
        .clientId("client-id")
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("{baseUrl}/api/public/auth/external/{registrationId}/callback")
        .scope("openid")
        .authorizationUri("https://issuer.example/authorize")
        .tokenUri("https://issuer.example/token")
        .jwkSetUri("https://issuer.example/jwks")
        .issuerUri("https://issuer.example")
        .userNameAttributeName("sub")
        .build()
    private val registrations = ClientRegistrationRepository { id -> registration.takeIf { id == "provider-1" } }
    private val transactionService = mock(IAuthenticationTransactionService::class.java)
    private val states = InMemoryExternalLoginStateStore()
    private val resolver = KudosOAuth2AuthorizationRequestResolver(registrations, transactionService, states)

    @Test
    fun resolverAddsPkceAndCorrelatesTransactionToState() {
        val now = Instant.now()
        `when`(transactionService.get("tx-1")).thenReturn(
            AuthenticationTransaction(
                id = "tx-1",
                tenantId = "t-1",
                status = AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED,
                nextActions = setOf(AuthenticationActionEnum.REDIRECT_EXTERNAL_PROVIDER),
                method = "external:provider-1",
                createdAt = now,
                updatedAt = now,
                expiresAt = now.plusSeconds(300),
            )
        )
        val request = MockHttpServletRequest().apply {
            scheme = "https"
            serverName = "app.example"
            serverPort = 443
            setParameter("transactionId", "tx-1")
        }

        val authorization = assertNotNull(resolver.resolve(request, "provider-1"))

        assertTrue(authorization.additionalParameters["code_challenge"].toString().isNotBlank())
        assertEquals("S256", authorization.additionalParameters["code_challenge_method"])
        val correlated = assertNotNull(states.consume(assertNotNull(authorization.state)))
        assertEquals("tx-1", correlated.transactionId)
        assertEquals("provider-1", correlated.providerId)
    }
}
