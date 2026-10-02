package io.kudos.ms.auth.core.authentication

import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionStatusEnum
import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction
import io.kudos.ms.auth.core.authentication.store.InMemoryAuthenticationTransactionStore
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.*

internal class InMemoryAuthenticationTransactionStoreTest {
    private class MutableClock(var now: Instant) : Clock() {
        override fun instant(): Instant = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
    }
    private val clock = MutableClock(Instant.parse("2026-10-01T00:00:00Z"))
    private fun transaction(id: String) = AuthenticationTransaction(
        id = id, tenantId = null, status = AuthenticationTransactionStatusEnum.WAITING_FOR_ACTION,
        createdAt = clock.instant(), updatedAt = clock.instant(), expiresAt = clock.instant().plusSeconds(30),
    )

    @Test
    fun abandonedTransactionsExpireAndReleaseCapacity() {
        val store = InMemoryAuthenticationTransactionStore(clock, maxEntries = 2)
        val first = transaction("first")
        assertTrue(store.create(first))
        assertTrue(store.create(transaction("second")))
        assertFailsWith<IllegalStateException> { store.create(transaction("overflow")) }
        clock.now = clock.now.plusSeconds(30)
        assertNull(store.get(first.id))
        assertNull(store.save(first, first.version))
        assertTrue(store.create(transaction("new")))
    }

    @Test
    fun compareAndSetRejectsStaleWritesAndLifetimeExtension() {
        val store = InMemoryAuthenticationTransactionStore(clock)
        val transaction = transaction("first")
        assertTrue(store.create(transaction))
        assertNull(store.save(transaction.copy(expiresAt = transaction.expiresAt.plusSeconds(1)), 0))
        assertEquals(1, store.save(transaction, 0)?.version)
        assertNull(store.save(transaction, 0))
        assertFalse(store.create(transaction))
    }
}
