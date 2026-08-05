package io.kudos.base.model.vo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * IdAndName test cases.
 *
 * Covers: generic id type support, data class equals/hashCode/copy/componentN, toString.
 *
 * @author K
 * @since 1.0.0
 */
internal class IdAndNameTest {

    @Test
    fun testConstructionWithStringId() {
        val v = IdAndName("id-1", "Alice")
        assertEquals("id-1", v.id)
        assertEquals("Alice", v.name)
    }

    @Test
    fun testConstructionWithIntId() {
        val v = IdAndName(100, "Bob")
        assertEquals(100, v.id)
        assertEquals("Bob", v.name)
    }

    @Test
    fun testEqualsAndHashCode() {
        val a = IdAndName(1L, "x")
        val b = IdAndName(1L, "x")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun testEqualsDistinguishesIdAndName() {
        val base = IdAndName(1, "x")
        assertNotEquals(base, IdAndName(2, "x"))
        assertNotEquals(base, IdAndName(1, "y"))
    }

    @Test
    fun testCopy() {
        val original = IdAndName("a", "name")
        val copied = original.copy(name = "name2")
        assertEquals("a", copied.id)
        assertEquals("name2", copied.name)
        assertEquals("name", original.name, "copy must not mutate original")
    }

    @Test
    fun testDestructuring() {
        val (id, name) = IdAndName(7, "seven")
        assertEquals(7, id)
        assertEquals("seven", name)
    }

    @Test
    fun testToStringContainsFields() {
        val s = IdAndName(5, "five").toString()
        assertEquals("IdAndName(id=5, name=five)", s)
    }
}
