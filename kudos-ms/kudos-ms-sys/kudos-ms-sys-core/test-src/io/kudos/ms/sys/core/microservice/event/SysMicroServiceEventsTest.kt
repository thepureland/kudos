package io.kudos.ms.sys.core.microservice.event

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Pure unit test for microservice (`sys_micro_service`) domain event data classes.
 * Focuses on the [SysMicroServiceEvent.id] contract (id == code) and the derived
 * [SysMicroServiceBatchDeleted.id] getter (returns first id, throws on empty).
 *
 * No Spring context or database required.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysMicroServiceEventsTest {

    @Test
    fun inserted_id() {
        assertEquals("c1", SysMicroServiceInserted(id = "c1").id)
    }

    @Test
    fun updated_id() {
        assertEquals("c2", SysMicroServiceUpdated(id = "c2").id)
    }

    @Test
    fun deleted_id() {
        assertEquals("c3", SysMicroServiceDeleted(id = "c3").id)
    }

    @Test
    fun batchDeleted_idReturnsFirst() {
        val e = SysMicroServiceBatchDeleted(ids = listOf("a", "b", "c"))
        assertEquals("a", e.id)
        assertEquals(listOf("a", "b", "c"), e.ids)
    }

    @Test
    fun batchDeleted_singleElement() {
        assertEquals("only", SysMicroServiceBatchDeleted(ids = listOf("only")).id)
    }

    @Test
    fun batchDeleted_emptyIds_idGetterThrows() {
        assertFailsWith<NoSuchElementException> {
            SysMicroServiceBatchDeleted(ids = emptyList()).id
        }
    }

    @Test
    fun batchDeleted_polymorphic_asSealedInterface() {
        val event: SysMicroServiceEvent = SysMicroServiceBatchDeleted(ids = listOf("x", "y"))
        assertEquals("x", event.id)
    }
}
