package io.kudos.ms.sys.core.resource.event

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pure unit test for resource (`sys_resource`) domain event data classes.
 * Focuses on the derived [SysResourceBatchDeleted.id] getter and field carrying.
 *
 * No Spring context or database required.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class ResourceEventsTest {

    @Test
    fun inserted_id() {
        assertEquals("i1", SysResourceInserted(id = "i1").id)
    }

    @Test
    fun updated_id() {
        assertEquals("u1", SysResourceUpdated(id = "u1").id)
    }

    @Test
    fun deleted_id() {
        assertEquals("d1", SysResourceDeleted(id = "d1").id)
    }

    @Test
    fun batchDeleted_idReturnsFirst() {
        val e = SysResourceBatchDeleted(ids = listOf("a", "b", "c"))
        assertEquals("a", e.id)
        assertEquals(3, e.ids.size)
    }
}
