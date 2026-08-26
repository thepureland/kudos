package io.kudos.ms.auth.core.authentication.mfa

import io.kudos.ms.auth.core.authentication.mfa.store.InMemoryTotpEnrollmentStore
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class InMemoryTotpEnrollmentStoreTest {

    @Test
    fun createCasAndConsumeAreAtomic() {
        val store = InMemoryTotpEnrollmentStore()
        val enrollment = enrollment()

        assertTrue(store.create(enrollment))
        assertFalse(store.create(enrollment))
        val updated = assertNotNull(store.save(enrollment.copy(failedAttempts = 1), 0))
        assertEquals(1, updated.version)
        assertNull(store.save(enrollment.copy(failedAttempts = 2), 0))
        assertNull(store.consume(enrollment.id, 0))
        assertEquals(updated, store.consume(enrollment.id, 1))
        assertNull(store.get(enrollment.id))
    }

    @Test
    fun anEnrollmentStopsBeingReadableTheMomentItsWindowCloses() {
        val start = Instant.parse("2026-08-25T10:00:00Z")
        val expiry = start.plusSeconds(60)
        val stillOpen = InMemoryTotpEnrollmentStore(Clock.fixed(expiry.minusSeconds(1), ZoneOffset.UTC))
        // Exactly at the expiry instant the enrollment is already gone: the window is half-open, matching the
        // Redis store, where the key is dropped by its own PEXPIREAT rather than by a Java-side comparison.
        val justClosed = InMemoryTotpEnrollmentStore(Clock.fixed(expiry, ZoneOffset.UTC))
        val enrollment = enrollment(createdAt = start, expiresAt = expiry)

        assertTrue(stillOpen.create(enrollment))
        assertTrue(justClosed.create(enrollment))

        assertNotNull(stillOpen.get(enrollment.id))
        assertNull(justClosed.get(enrollment.id))
        // Reading an expired enrollment also drops it, so the map does not keep dead entries around.
        assertNull(justClosed.consume(enrollment.id, 0))
    }

    private fun enrollment(
        createdAt: Instant = Instant.now(),
        expiresAt: Instant = createdAt.plusSeconds(60),
    ) = TotpEnrollment(
        id = "enrollment-1",
        tenantId = "t-1",
        userId = "u-1",
        encryptedSecret = "encrypted",
        createdAt = createdAt,
        expiresAt = expiresAt,
    )
}
