package io.kudos.ms.sys.core.param.event

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Pure unit test for parameter (`sys_param`) event data classes. Focuses on the delete events carrying the
 * composite (atomicServiceCode, paramName) cache dimension and the derived [SysParamBatchDeleted.id] getter.
 *
 * No Spring context or database required.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysParamEventsTest {

    @Test
    fun inserted_id() {
        assertEquals("i1", SysParamInserted(id = "i1").id)
    }

    @Test
    fun updated_id() {
        assertEquals("u1", SysParamUpdated(id = "u1").id)
    }

    @Test
    fun deleted_carriesCompositeDimension() {
        val e = SysParamDeleted(id = "d1", atomicServiceCode = "svc-a", paramName = "max.size")
        assertEquals("d1", e.id)
        assertEquals("svc-a", e.atomicServiceCode)
        assertEquals("max.size", e.paramName)
    }

    @Test
    fun batchDeleted_idReturnsFirst_carriesModuleAndNames() {
        val pairs = listOf("svc-a" to "p1", "svc-b" to "p2")
        val e = SysParamBatchDeleted(ids = listOf("a", "b"), moduleAndNames = pairs)
        assertEquals("a", e.id)
        assertEquals(pairs, e.moduleAndNames)
    }

    @Test
    fun batchDeleted_emptyIds_idGetterThrows() {
        assertFailsWith<NoSuchElementException> {
            SysParamBatchDeleted(ids = emptyList(), moduleAndNames = emptyList()).id
        }
    }
}
