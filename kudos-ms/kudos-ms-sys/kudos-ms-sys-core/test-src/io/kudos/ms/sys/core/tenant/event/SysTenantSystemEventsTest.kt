package io.kudos.ms.sys.core.tenant.event

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for the tenant-system relation event data classes in TenantSystemEvents.kt (pure logic, no Spring container)
 *
 * Coverage:
 * - SysTenantSystemBound carries id / tenantId / systemCode unchanged
 * - SysTenantSystemTenantsChanged / SysTenantSystemSystemsChanged carry their collections (incl. empty collections)
 * - data class equality / copy / toString
 *
 * @author K
 * @since 1.0.0
 */
internal class SysTenantSystemEventsTest {

    @Test
    fun bound_carriesAllProperties() {
        val event = SysTenantSystemBound(id = "rel-1", tenantId = "t-1", systemCode = "sys-a")
        assertEquals("rel-1", event.id)
        assertEquals("t-1", event.tenantId)
        assertEquals("sys-a", event.systemCode)
        assertEquals(event, event.copy())
        assertNotEquals(event, event.copy(systemCode = "sys-b"))
        assertTrue(event.toString().contains("rel-1"))
    }

    @Test
    fun tenantsChanged_carriesTenantIds() {
        val event = SysTenantSystemTenantsChanged(tenantIds = listOf("t-1", "t-2"))
        assertEquals(listOf("t-1", "t-2"), event.tenantIds.toList())
        assertEquals(event, event.copy())
        assertTrue(SysTenantSystemTenantsChanged(emptyList()).tenantIds.isEmpty())
    }

    @Test
    fun systemsChanged_carriesSystemCodes() {
        val event = SysTenantSystemSystemsChanged(systemCodes = setOf("sys-a", "sys-b"))
        assertEquals(setOf("sys-a", "sys-b"), event.systemCodes.toSet())
        assertEquals(event, event.copy())
        assertTrue(SysTenantSystemSystemsChanged(emptySet()).systemCodes.isEmpty())
    }

    @Test
    fun allImplementSealedInterface() {
        val events: List<SysTenantSystemEvent> = listOf(
            SysTenantSystemBound("1", "t", "s"),
            SysTenantSystemTenantsChanged(listOf("t")),
            SysTenantSystemSystemsChanged(listOf("s")),
        )
        assertEquals(3, events.size)
    }
}
