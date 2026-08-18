package io.kudos.ms.sys.core.tenant.event

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * test for the tenant domain event data classes in TenantEvents.kt (pure logic, no Spring container)
 *
 * Coverage:
 * - SysTenantInserted / SysTenantUpdated / SysTenantDeleted carry the id unchanged
 * - SysTenantBatchDeleted.id falls back to ids.first(); empty ids throws NoSuchElementException
 * - data class equality / copy / toString of each event
 *
 * @author K
 * @since 1.0.0
 */
internal class SysTenantEventsTest {

    @Test
    fun singleIdEvents_carryId() {
        assertEquals("id-1", SysTenantInserted("id-1").id)
        assertEquals("id-2", SysTenantUpdated("id-2").id)
        assertEquals("id-3", SysTenantDeleted("id-3").id)
    }

    @Test
    fun batchDeleted_idIsFirstOfIds() {
        val event = SysTenantBatchDeleted(listOf("a", "b", "c"))
        assertEquals("a", event.id)
        assertEquals(listOf("a", "b", "c"), event.ids.toList())
    }

    @Test
    fun batchDeleted_emptyIdsThrowsOnIdAccess() {
        val event = SysTenantBatchDeleted(emptyList())
        assertFailsWith<NoSuchElementException> { event.id }
    }

    @Test
    fun dataClassSemantics() {
        val a = SysTenantInserted("x")
        val b = a.copy(id = "x")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(id = "y"))
        assertTrue(a.toString().contains("x"))

        val updated = SysTenantUpdated("u")
        assertEquals(updated, updated.copy())
        assertTrue(updated.toString().contains("u"))

        val deleted = SysTenantDeleted("d")
        assertEquals(deleted, deleted.copy())
        assertTrue(deleted.toString().contains("d"))

        val batch = SysTenantBatchDeleted(listOf("1"))
        assertEquals(batch, batch.copy())
        assertEquals(batch.hashCode(), batch.copy().hashCode())
        assertTrue(batch.toString().contains("1"))
    }

    @Test
    fun eventsAreAllSysTenantEvent() {
        val events: List<SysTenantEvent> = listOf(
            SysTenantInserted("1"),
            SysTenantUpdated("1"),
            SysTenantDeleted("1"),
            SysTenantBatchDeleted(listOf("1")),
        )
        assertTrue(events.all { it.id == "1" })
    }
}
