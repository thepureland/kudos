package io.kudos.ms.sys.core.system.event

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * test for the system domain event data classes in SystemEvents.kt (pure logic, no Spring container)
 *
 * Coverage:
 * - SysSystemInserted / SysSystemUpdated / SysSystemDeleted carry the code (id) unchanged
 * - SysSystemBatchDeleted.id falls back to ids.first(); empty ids throws NoSuchElementException
 * - data class equality / copy / toString
 *
 * @author K
 * @since 1.0.0
 */
internal class SystemEventsTest {

    @Test
    fun singleIdEvents_carryCode() {
        assertEquals("code-1", SysSystemInserted("code-1").id)
        assertEquals("code-2", SysSystemUpdated("code-2").id)
        assertEquals("code-3", SysSystemDeleted("code-3").id)
    }

    @Test
    fun batchDeleted_idIsFirstOfIds() {
        val event = SysSystemBatchDeleted(listOf("c1", "c2"))
        assertEquals("c1", event.id)
        assertEquals(listOf("c1", "c2"), event.ids.toList())
        assertFailsWith<NoSuchElementException> { SysSystemBatchDeleted(emptyList()).id }
    }

    @Test
    fun dataClassSemantics() {
        val a = SysSystemInserted("x")
        assertEquals(a, a.copy())
        assertEquals(a.hashCode(), a.copy().hashCode())
        assertNotEquals(a, a.copy(id = "y"))
        assertTrue(a.toString().contains("x"))
        assertEquals(SysSystemUpdated("u"), SysSystemUpdated("u").copy())
        assertEquals(SysSystemDeleted("d"), SysSystemDeleted("d").copy())
        assertEquals(SysSystemBatchDeleted(listOf("1")), SysSystemBatchDeleted(listOf("1")).copy())
    }

    @Test
    fun eventsAreAllSysSystemEvent() {
        val events: List<SysSystemEvent> = listOf(
            SysSystemInserted("1"),
            SysSystemUpdated("1"),
            SysSystemDeleted("1"),
            SysSystemBatchDeleted(listOf("1")),
        )
        assertTrue(events.all { it.id == "1" })
    }
}
