package io.kudos.ms.sys.core.domain.event

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Pure unit test for domain (`sys_domain`) event data classes. Focuses on the delete events carrying the
 * domain string (the cache is keyed by domain name) and the derived [SysDomainBatchDeleted.id] getter.
 *
 * No Spring context or database required.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SysDomainEventsTest {

    @Test
    fun inserted_id() {
        assertEquals("i1", SysDomainInserted(id = "i1").id)
    }

    @Test
    fun updated_id() {
        assertEquals("u1", SysDomainUpdated(id = "u1").id)
    }

    @Test
    fun deleted_carriesDomain() {
        val e = SysDomainDeleted(id = "d1", domain = "example.com")
        assertEquals("d1", e.id)
        assertEquals("example.com", e.domain)
    }

    @Test
    fun batchDeleted_idReturnsFirst_andCarriesDomains() {
        val e = SysDomainBatchDeleted(ids = listOf("a", "b"), domains = setOf("x.com", "y.com"))
        assertEquals("a", e.id)
        assertEquals(setOf("x.com", "y.com"), e.domains)
    }

    @Test
    fun batchDeleted_emptyIds_idGetterThrows() {
        assertFailsWith<NoSuchElementException> {
            SysDomainBatchDeleted(ids = emptyList(), domains = emptySet()).id
        }
    }

    @Test
    fun batchDeleted_emptyDomainsAllowed() {
        val e = SysDomainBatchDeleted(ids = listOf("only"), domains = emptySet())
        assertEquals("only", e.id)
        assertEquals(0, e.domains.size)
        assertNull(e.domains.firstOrNull())
    }
}
