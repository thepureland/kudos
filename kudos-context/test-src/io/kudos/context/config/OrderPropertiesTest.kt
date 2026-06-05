package io.kudos.context.config

import kotlin.test.*

/**
 * test for OrderProperties
 *
 * Verifies the core design goal: iteration over property names / keys / entries preserves
 * insertion order (plain [java.util.Properties] backed by a Hashtable does not).
 *
 * @author K
 * @since 1.0.0
 */
internal class OrderPropertiesTest {

    private fun sample() = OrderProperties().apply {
        // deliberately not alphabetical, to prove order is insertion order not hash/sort order
        setProperty("zebra", "1")
        setProperty("apple", "2")
        setProperty("mango", "3")
    }

    @Test
    fun valuesAreStoredAndRetrievable() {
        val p = sample()
        assertEquals("1", p.getProperty("zebra"))
        assertEquals("2", p.getProperty("apple"))
        assertEquals("3", p.getProperty("mango"))
    }

    @Test
    fun stringPropertyNamesPreserveInsertionOrder() {
        assertEquals(listOf("zebra", "apple", "mango"), sample().stringPropertyNames().toList())
    }

    @Test
    fun keysEnumerationPreservesInsertionOrder() {
        val keys = sample().keys().toList()
        assertEquals(listOf("zebra", "apple", "mango"), keys)
    }

    @Test
    fun entriesPreserveInsertionOrder() {
        val entries = sample().entries.map { it.key to it.value }
        assertEquals(listOf("zebra" to "1", "apple" to "2", "mango" to "3"), entries)
    }

    @Test
    fun reInsertingExistingKeyDoesNotDuplicateOrReorder() {
        val p = sample()
        p.setProperty("apple", "20") // update existing
        assertEquals(listOf("zebra", "apple", "mango"), p.stringPropertyNames().toList())
        assertEquals("20", p.getProperty("apple"))
    }

    @Test
    fun removeDropsKeyFromOrdering() {
        val p = sample()
        p.remove("apple")
        assertEquals(listOf("zebra", "mango"), p.stringPropertyNames().toList())
        assertNull(p.getProperty("apple"))
    }

    @Test
    fun clearEmptiesEverything() {
        val p = sample()
        p.clear()
        assertTrue(p.stringPropertyNames().isEmpty())
        assertFalse(p.keys().hasMoreElements())
    }
}
