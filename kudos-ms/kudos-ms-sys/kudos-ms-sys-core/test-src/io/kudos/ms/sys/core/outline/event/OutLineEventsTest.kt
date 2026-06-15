package io.kudos.ms.sys.core.outline.event

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pure unit test for outbound-allowlist (`sys_out_line`) domain event data classes.
 * Focuses on the derived [SysOutLineBatchDeleted.id] getter and field carrying.
 *
 * No Spring context or database required.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class OutLineEventsTest {

    @Test
    fun inserted_id() {
        assertEquals("i1", SysOutLineInserted(id = "i1").id)
    }

    @Test
    fun updated_id() {
        assertEquals("u1", SysOutLineUpdated(id = "u1").id)
    }

    @Test
    fun deleted_fields() {
        val e = SysOutLineDeleted(id = "d1", systemCode = "s", tenantId = "t")
        assertEquals("d1", e.id)
        assertEquals("s", e.systemCode)
        assertEquals("t", e.tenantId)
    }

    @Test
    fun deleted_nullTenant() {
        val e = SysOutLineDeleted(id = "d2", systemCode = "s", tenantId = null)
        assertEquals(null, e.tenantId)
    }

    @Test
    fun batchDeleted_idReturnsFirst() {
        val e = SysOutLineBatchDeleted(
            ids = listOf("a", "b"),
            dimensions = setOf("s" to "t", "s2" to null),
        )
        assertEquals("a", e.id)
        assertEquals(2, e.dimensions.size)
    }
}
