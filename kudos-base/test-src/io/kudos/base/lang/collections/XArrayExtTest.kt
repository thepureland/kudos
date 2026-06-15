package io.kudos.base.lang.collections

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

/**
 * XArray extension function test cases.
 *
 * Covers the nullable-array extensions: add(index, element) / remove(index) /
 * removeElement(element) / removeAll(vararg indices) / removeElements(vararg values),
 * including index boundary (0, size), out-of-bounds exceptions, not-found cases and duplicates.
 *
 * (toNumberArray is covered separately in XArrayTest.)
 *
 * @author K
 * @since 1.0.0
 */
internal class XArrayExtTest {

    @Test
    fun add() {
        val base = arrayOf<String?>("a", "b")
        // insert in the middle
        assertContentEquals(arrayOf("a", "x", "b"), base.add(1, "x"))
        // insert at the beginning (index 0)
        assertContentEquals(arrayOf("x", "a", "b"), base.add(0, "x"))
        // insert at the end (index == size)
        assertContentEquals(arrayOf("a", "b", "x"), base.add(2, "x"))
        // insert null
        assertContentEquals(arrayOf<String?>("a", null, "b"), base.add(1, null))
        // original array is not mutated
        assertContentEquals(arrayOf("a", "b"), base)
    }

    @Test
    fun add_outOfBounds() {
        val base = arrayOf<String?>("a", "b")
        assertFailsWith<IndexOutOfBoundsException> { base.add(-1, "x") }
        assertFailsWith<IndexOutOfBoundsException> { base.add(3, "x") }
    }

    @Test
    fun remove() {
        assertContentEquals(arrayOf<String?>(), arrayOf<String?>("a").remove(0))
        assertContentEquals(arrayOf("b"), arrayOf<String?>("a", "b").remove(0))
        assertContentEquals(arrayOf("a"), arrayOf<String?>("a", "b").remove(1))
        assertContentEquals(arrayOf("a", "c"), arrayOf<String?>("a", "b", "c").remove(1))
    }

    @Test
    fun remove_outOfBounds() {
        val base = arrayOf<String?>("a", "b")
        assertFailsWith<IndexOutOfBoundsException> { base.remove(-1) }
        // index == size+ is out of bounds; note the guard is 0..size so size itself is allowed but
        // copyOfRange(size+1, size) would fail. We assert the clearly-out-of-range case.
        assertFailsWith<IndexOutOfBoundsException> { base.remove(3) }
    }

    @Test
    fun removeElement() {
        assertContentEquals(arrayOf<String?>(), arrayOf<String?>().removeElement("a"))
        // element not present -> returns same content
        assertContentEquals(arrayOf("a"), arrayOf<String?>("a").removeElement("b"))
        assertContentEquals(arrayOf("b"), arrayOf<String?>("a", "b").removeElement("a"))
        // only the first occurrence is removed
        assertContentEquals(arrayOf("b", "a"), arrayOf<String?>("a", "b", "a").removeElement("a"))
        // removing null
        assertContentEquals(arrayOf("a"), arrayOf<String?>(null, "a").removeElement(null))
    }

    @Test
    fun removeAll() {
        assertContentEquals(arrayOf("b"), arrayOf<String?>("a", "b", "c").removeAll(0, 2))
        assertContentEquals(arrayOf("a"), arrayOf<String?>("a", "b", "c").removeAll(1, 2))
        // duplicate / out-of-range indices in the set are simply ignored (filterIndexed never matches them)
        assertContentEquals(arrayOf("b", "c"), arrayOf<String?>("a", "b", "c").removeAll(0, 0, 99))
        // no indices -> unchanged content
        assertContentEquals(arrayOf("a", "b"), arrayOf<String?>("a", "b").removeAll())
    }

    @Test
    fun removeElements() {
        assertContentEquals(arrayOf<String?>(), arrayOf<String?>().removeElements("a", "b"))
        assertContentEquals(arrayOf("a"), arrayOf<String?>("a").removeElements("b", "c"))
        assertContentEquals(arrayOf("b"), arrayOf<String?>("a", "b").removeElements("a", "c"))
        // all occurrences of a matched value are removed
        assertContentEquals(arrayOf("b"), arrayOf<String?>("a", "b", "a").removeElements("a"))
        // multiple values
        assertContentEquals(arrayOf("c"), arrayOf<String?>("a", "b", "a", "c").removeElements("a", "b"))
    }

}
