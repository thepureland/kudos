package io.kudos.ms.auth.provider.webauthn.ceremony

import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryWebAuthnCeremonyStoreTest {

    @Test
    fun `should create read and consume state exactly once`() {
        val clock = MutableClock(Instant.parse("2026-08-25T00:00:00Z"))
        val store = InMemoryWebAuthnCeremonyStore(clock)
        val state = state("ceremony-1", clock.instant().plusSeconds(60))

        assertTrue(store.create(state))
        assertEquals(state, store.get(state.id))
        assertEquals(state, store.consume(state.id))
        assertNull(store.get(state.id))
        assertNull(store.consume(state.id))
    }

    @Test
    fun `should reject duplicate and already expired state`() {
        val clock = MutableClock(Instant.parse("2026-08-25T00:00:00Z"))
        val store = InMemoryWebAuthnCeremonyStore(clock)
        val state = state("ceremony-1", clock.instant().plusSeconds(60))

        assertTrue(store.create(state))
        assertFalse(store.create(state.copy(userId = "other-user")))
        assertFalse(store.create(state("expired", clock.instant())))
    }

    @Test
    fun `should remove state after expiration`() {
        val clock = MutableClock(Instant.parse("2026-08-25T00:00:00Z"))
        val store = InMemoryWebAuthnCeremonyStore(clock)
        val state = state("ceremony-1", clock.instant().plusSeconds(60))
        assertTrue(store.create(state))

        clock.advanceSeconds(60)

        assertNull(store.get(state.id))
        assertNull(store.consume(state.id))
        assertTrue(store.create(state("ceremony-1", clock.instant().plusSeconds(60))))
    }

    private fun state(id: String, expiresAt: Instant) = WebAuthnCeremonyState(
        id = id,
        type = WebAuthnCeremonyTypeEnum.REGISTRATION,
        tenantId = "tenant-1",
        userId = "user-1",
        requestJson = "{}",
        createdAt = expiresAt.minusSeconds(60),
        expiresAt = expiresAt,
    )

    private class MutableClock(
        private var instant: Instant,
        private val zone: ZoneId = ZoneOffset.UTC,
    ) : Clock() {
        override fun getZone(): ZoneId = zone

        override fun withZone(zone: ZoneId): Clock = MutableClock(instant, zone)

        override fun instant(): Instant = instant

        fun advanceSeconds(seconds: Long) {
            instant = instant.plusSeconds(seconds)
        }
    }
}
