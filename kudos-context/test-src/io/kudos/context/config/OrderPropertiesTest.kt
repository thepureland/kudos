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

    // ============================================================
    // putIfAbsent / remove(key, value): the Hashtable bypass overrides
    // ============================================================

    @Test
    fun putIfAbsentAddsNewKeyToOrderedViews() {
        val p = sample()
        assertNull(p.putIfAbsent("kiwi", "4"), "no previous value -> null")
        assertEquals(listOf("zebra", "apple", "mango", "kiwi"), p.stringPropertyNames().toList())
        assertEquals("4", p.getProperty("kiwi"))
    }

    @Test
    fun putIfAbsentKeepsExistingValueAndOrdering() {
        val p = sample()
        assertEquals("2", p.putIfAbsent("apple", "999"), "the existing value must be returned")
        assertEquals("2", p.getProperty("apple"), "the existing value must be kept")
        assertEquals(listOf("zebra", "apple", "mango"), p.stringPropertyNames().toList(), "no duplicate ordering entry")
    }

    @Test
    fun twoArgRemoveDropsKeyOnlyOnValueMatch() {
        val p = sample()
        assertFalse(p.remove("apple", "wrong-value"), "non-matching value -> no removal")
        assertEquals(listOf("zebra", "apple", "mango"), p.stringPropertyNames().toList())

        assertTrue(p.remove("apple", "2"), "matching value -> removed")
        assertEquals(listOf("zebra", "mango"), p.stringPropertyNames().toList())
        assertNull(p.getProperty("apple"))
    }

    @Test
    fun putReturnsPreviousValueAndRemoveReturnsRemovedValue() {
        val p = sample()
        assertEquals("1", p.put("zebra", "10"))
        assertNull(p.put("new", "n"))
        assertEquals("10", p.remove("zebra"))
        assertNull(p.remove("not-there"))
    }

    // ============================================================
    // propertyNames / keys property (keySet)
    // ============================================================

    @Test
    fun propertyNamesEnumerationPreservesInsertionOrder() {
        val names = sample().propertyNames().toList()
        assertEquals(listOf("zebra", "apple", "mango"), names)
    }

    @Test
    fun keysPropertyReflectsLiveStateInInsertionOrder() {
        val p = sample()
        assertEquals(listOf<Any>("zebra", "apple", "mango"), p.keys.toList(), "keySet must not be an empty construction-time snapshot")
        p.setProperty("delta", "4")
        assertEquals(listOf<Any>("zebra", "apple", "mango", "delta"), p.keys.toList(), "keySet must see later insertions")
    }

    // ============================================================
    // defaults (the secondary constructor)
    // ============================================================

    private fun withDefaults(): OrderProperties {
        val defaults = OrderProperties().apply {
            setProperty("fallback", "df")
            setProperty("apple", "default-apple") // overridden by the main properties
        }
        return OrderProperties(defaults).apply {
            setProperty("zebra", "1")
            setProperty("apple", "2")
        }
    }

    @Test
    fun getPropertyFallsThroughToDefaults() {
        val p = withDefaults()
        assertEquals("df", p.getProperty("fallback"), "unset keys must fall through to defaults")
        assertEquals("2", p.getProperty("apple"), "own values must shadow defaults")
    }

    @Test
    fun stringPropertyNamesIncludeDefaults() {
        val names = withDefaults().stringPropertyNames().toList()
        assertEquals(listOf("zebra", "apple", "fallback"), names, "own keys first, then non-shadowed default keys")
    }

    @Test
    fun entriesIncludeNonShadowedDefaultsOnly() {
        val entries = withDefaults().entries.associate { it.key to it.value }
        assertEquals("1", entries["zebra"])
        assertEquals("2", entries["apple"], "the shadowing value must win over the default")
        assertEquals("df", entries["fallback"], "non-shadowed defaults must be included")
        assertEquals(3, entries.size)
    }

    @Test
    fun nullDefaultsConstructorBehavesLikeEmpty() {
        val p = OrderProperties(null)
        p.setProperty("only", "1")
        assertEquals(listOf("only"), p.stringPropertyNames().toList())
        assertEquals(listOf("only" to "1"), p.entries.map { it.key to it.value })
    }
}
