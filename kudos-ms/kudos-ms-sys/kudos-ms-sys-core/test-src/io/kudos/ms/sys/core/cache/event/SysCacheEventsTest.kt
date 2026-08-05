package io.kudos.ms.sys.core.cache.event

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Pure unit test for cache configuration (`sys_cache`) event data classes. Focuses on the
 * [SysCacheEvent.id] contract and the derived [SysCacheBatchDeleted.id] getter.
 *
 * No Spring context or database required.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysCacheEventsTest {

    @Test
    fun inserted_id() {
        assertEquals("i1", SysCacheInserted(id = "i1").id)
    }

    @Test
    fun updated_id() {
        assertEquals("u1", SysCacheUpdated(id = "u1").id)
    }

    @Test
    fun deleted_id() {
        assertEquals("d1", SysCacheDeleted(id = "d1").id)
    }

    @Test
    fun batchDeleted_idReturnsFirst() {
        val e = SysCacheBatchDeleted(ids = listOf("a", "b", "c"))
        assertEquals("a", e.id)
        assertEquals(3, e.ids.size)
    }

    @Test
    fun batchDeleted_emptyIds_idGetterThrows() {
        assertFailsWith<NoSuchElementException> {
            SysCacheBatchDeleted(ids = emptyList()).id
        }
    }

    @Test
    fun polymorphic_asSealedInterface() {
        val event: SysCacheEvent = SysCacheUpdated(id = "x")
        assertEquals("x", event.id)
    }
}
