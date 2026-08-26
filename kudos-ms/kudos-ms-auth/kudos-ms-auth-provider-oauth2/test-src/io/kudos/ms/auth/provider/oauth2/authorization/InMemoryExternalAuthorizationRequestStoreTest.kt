package io.kudos.ms.auth.provider.oauth2.authorization

import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class InMemoryExternalAuthorizationRequestStoreTest {

    @Test
    fun snapshotHasCollisionProtectionAndCanBeConsumedOnlyOnce() {
        val clock = MutableClock(Instant.parse("2026-08-24T00:00:00Z"))
        val store = InMemoryExternalAuthorizationRequestStore(clock)
        val request = authorizationRequest("state-1")

        assertTrue(store.create(request, clock.instant().plusSeconds(60)))
        assertFalse(store.create(request, clock.instant().plusSeconds(60)))
        assertEquals(request, store.get("state-1"))
        assertEquals(request, store.consume("state-1"))
        assertNull(store.consume("state-1"))
    }

    @Test
    fun expiredSnapshotIsNeitherLoadedNorConsumed() {
        val clock = MutableClock(Instant.parse("2026-08-24T00:00:00Z"))
        val store = InMemoryExternalAuthorizationRequestStore(clock)
        val request = authorizationRequest("state-expired")
        assertTrue(store.create(request, clock.instant().plusSeconds(1)))

        clock.now = clock.instant().plusSeconds(1)

        assertNull(store.get("state-expired"))
        assertNull(store.consume("state-expired"))
        assertTrue(store.create(request, clock.instant().plusSeconds(1)))
    }

    private fun authorizationRequest(state: String) = OAuth2AuthorizationRequest.authorizationCode()
        .authorizationUri("https://issuer.example/authorize")
        .clientId("client")
        .redirectUri("https://app.example/callback")
        .scopes(setOf("openid"))
        .state(state)
        .build()

    private class MutableClock(var now: Instant) : Clock() {
        override fun instant(): Instant = now
        override fun getZone(): ZoneId = ZoneId.of("UTC")
        override fun withZone(zone: ZoneId): Clock = this
    }
}
