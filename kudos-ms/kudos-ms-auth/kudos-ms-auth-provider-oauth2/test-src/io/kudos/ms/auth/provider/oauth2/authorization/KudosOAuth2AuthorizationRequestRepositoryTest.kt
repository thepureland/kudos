package io.kudos.ms.auth.provider.oauth2.authorization

import io.kudos.ms.auth.provider.oauth2.init.ExternalLoginProperties
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

internal class KudosOAuth2AuthorizationRequestRepositoryTest {
    private val now = Instant.parse("2026-08-24T00:00:00Z")
    private val store = InMemoryExternalAuthorizationRequestStore(Clock.fixed(now, ZoneOffset.UTC))
    private val properties = ExternalLoginProperties().apply { authorizationRequestTtlSeconds = 60 }
    private val repository = KudosOAuth2AuthorizationRequestRepository(
        store,
        properties,
        Clock.fixed(now, ZoneOffset.UTC),
    )
    private val response = MockHttpServletResponse()

    @Test
    fun saveLoadAndRemovePreservePkceAndNonceWithoutCreatingSession() {
        val authorization = authorizationRequest("state-1")
        repository.saveAuthorizationRequest(authorization, MockHttpServletRequest(), response)
        val callback = MockHttpServletRequest().apply { setParameter("state", "state-1") }

        assertEquals(authorization, repository.loadAuthorizationRequest(callback))
        assertEquals("verifier", repository.loadAuthorizationRequest(callback)?.getAttribute("code_verifier"))
        assertEquals("nonce-1", repository.loadAuthorizationRequest(callback)?.additionalParameters?.get("nonce"))
        assertNull(callback.getSession(false))

        assertEquals(authorization, repository.removeAuthorizationRequest(callback, response))
        assertNull(repository.removeAuthorizationRequest(callback, response))
        assertNull(callback.getSession(false))
    }

    @Test
    fun invalidCallbackStateIsIgnored() {
        val oversized = MockHttpServletRequest().apply { setParameter("state", "x".repeat(513)) }
        assertNull(repository.removeAuthorizationRequest(oversized, response))
    }

    @Test
    fun duplicateStateAndUnsafeTtlFailClosed() {
        val authorization = authorizationRequest("state-collision")
        repository.saveAuthorizationRequest(authorization, MockHttpServletRequest(), response)
        assertFailsWith<IllegalStateException> {
            repository.saveAuthorizationRequest(authorization, MockHttpServletRequest(), response)
        }

        properties.authorizationRequestTtlSeconds = 0
        assertFailsWith<IllegalArgumentException> {
            repository.saveAuthorizationRequest(authorizationRequest("state-ttl"), MockHttpServletRequest(), response)
        }
    }

    private fun authorizationRequest(state: String) = OAuth2AuthorizationRequest.authorizationCode()
        .authorizationUri("https://issuer.example/authorize")
        .clientId("client")
        .redirectUri("https://app.example/callback")
        .scopes(setOf("openid", "profile"))
        .state(state)
        .attributes { attributes ->
            attributes["registration_id"] = "google"
            attributes["code_verifier"] = "verifier"
        }
        .additionalParameters { parameters ->
            parameters["code_challenge"] = "challenge"
            parameters["code_challenge_method"] = "S256"
            parameters["nonce"] = "nonce-1"
        }
        .build()
}
