package io.kudos.ms.sys.core.datasource.event

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Pure unit test for data source (`sys_data_source`) event data classes. Focuses on the
 * [SysDataSourceEvent.id] contract and the derived [SysDataSourceBatchDeleted.id] getter.
 *
 * No Spring context or database required.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysDataSourceEventsTest {

    @Test
    fun inserted_id() {
        assertEquals("i1", SysDataSourceInserted(id = "i1").id)
    }

    @Test
    fun updated_id() {
        assertEquals("u1", SysDataSourceUpdated(id = "u1").id)
    }

    @Test
    fun deleted_id() {
        assertEquals("d1", SysDataSourceDeleted(id = "d1").id)
    }

    @Test
    fun batchDeleted_idReturnsFirst() {
        val e = SysDataSourceBatchDeleted(ids = listOf("a", "b"))
        assertEquals("a", e.id)
        assertEquals(2, e.ids.size)
    }

    @Test
    fun batchDeleted_emptyIds_idGetterThrows() {
        assertFailsWith<NoSuchElementException> {
            SysDataSourceBatchDeleted(ids = emptyList()).id
        }
    }
}
