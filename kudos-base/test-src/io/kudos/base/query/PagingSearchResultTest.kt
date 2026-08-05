package io.kudos.base.query

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * PagingSearchResult test cases.
 *
 * Covers: field access, generic element type, empty result, data class equals/hashCode/copy/componentN.
 *
 * @author K
 * @since 1.0.0
 */
internal class PagingSearchResultTest {

    @Test
    fun testFields() {
        val result = PagingSearchResult(listOf("a", "b"), 10)
        assertEquals(listOf("a", "b"), result.data)
        assertEquals(10, result.totalCount)
    }

    @Test
    fun testEmptyResult() {
        val result = PagingSearchResult(emptyList<Int>(), 0)
        assertTrue(result.data.isEmpty())
        assertEquals(0, result.totalCount)
    }

    @Test
    fun testTotalCountMayExceedDataSizeForPagedQueries() {
        // data is a single page; totalCount is the overall row count
        val result = PagingSearchResult(listOf(1, 2, 3), 57)
        assertEquals(3, result.data.size)
        assertEquals(57, result.totalCount)
    }

    @Test
    fun testEqualsAndHashCode() {
        val a = PagingSearchResult(listOf("x"), 1)
        val b = PagingSearchResult(listOf("x"), 1)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun testEqualsDistinguishesFields() {
        val base = PagingSearchResult(listOf("x"), 1)
        assertNotEquals(base, PagingSearchResult(listOf("y"), 1))
        assertNotEquals(base, PagingSearchResult(listOf("x"), 2))
    }

    @Test
    fun testCopyAndDestructuring() {
        val original = PagingSearchResult(listOf(1), 1)
        val copied = original.copy(totalCount = 99)
        assertEquals(listOf(1), copied.data)
        assertEquals(99, copied.totalCount)
        val (data, count) = copied
        assertEquals(listOf(1), data)
        assertEquals(99, count)
    }
}
