package io.kudos.ms.auth.common.grant.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for GrantRequestStatus
 *
 * Covers entries/count, [GrantRequestStatus.isTerminal] for every state, and the
 * [GrantRequestStatus.fromString] parser (trim, case-insensitivity, null/blank/unknown).
 *
 * @author K
 * @since 1.0.0
 */
internal class GrantRequestStatusTest {

    @Test
    fun entries() {
        assertEquals(4, GrantRequestStatus.entries.size)
        assertEquals(
            listOf("PENDING", "APPROVED", "REJECTED", "CANCELLED"),
            GrantRequestStatus.entries.map { it.name },
        )
    }

    @Test
    fun isTerminal() {
        assertFalse(GrantRequestStatus.PENDING.isTerminal())
        assertTrue(GrantRequestStatus.APPROVED.isTerminal())
        assertTrue(GrantRequestStatus.REJECTED.isTerminal())
        assertTrue(GrantRequestStatus.CANCELLED.isTerminal())
    }

    @Test
    fun fromStringExactMatch() {
        assertSame(GrantRequestStatus.PENDING, GrantRequestStatus.fromString("PENDING"))
        assertSame(GrantRequestStatus.APPROVED, GrantRequestStatus.fromString("APPROVED"))
        assertSame(GrantRequestStatus.REJECTED, GrantRequestStatus.fromString("REJECTED"))
        assertSame(GrantRequestStatus.CANCELLED, GrantRequestStatus.fromString("CANCELLED"))
    }

    @Test
    fun fromStringTrimsAndIsCaseInsensitive() {
        assertSame(GrantRequestStatus.PENDING, GrantRequestStatus.fromString("pending"))
        assertSame(GrantRequestStatus.APPROVED, GrantRequestStatus.fromString("  Approved  "))
        assertSame(GrantRequestStatus.CANCELLED, GrantRequestStatus.fromString("\tcancelled\n"))
    }

    @Test
    fun fromStringUnknownOrNullReturnsNull() {
        assertNull(GrantRequestStatus.fromString(null))
        assertNull(GrantRequestStatus.fromString(""))
        assertNull(GrantRequestStatus.fromString("   "))
        assertNull(GrantRequestStatus.fromString("DONE"))
    }

    @Test
    fun valueOf() {
        assertEquals(GrantRequestStatus.REJECTED, GrantRequestStatus.valueOf("REJECTED"))
    }
}
