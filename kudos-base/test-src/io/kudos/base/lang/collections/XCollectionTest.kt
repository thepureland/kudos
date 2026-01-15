package io.kudos.base.lang.collections

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * XCollection测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class XCollectionTest {

    @Test
    fun testIsEqualCollection_SameElements() {
        val list1 = listOf(1, 2, 3, 4, 5)
        val list2 = listOf(1, 2, 3, 4, 5)
        assertTrue(list1.isEqualCollection(list2))
    }

    @Test
    fun testIsEqualCollection_DifferentOrder() {
        val list1 = listOf(1, 2, 3, 4, 5)
        val list2 = listOf(5, 4, 3, 2, 1)
        assertTrue(list1.isEqualCollection(list2))
    }

    @Test
    fun testIsEqualCollection_DuplicateElements() {
        val list1 = listOf(1, 2, 2, 3)
        val list2 = listOf(1, 2, 2, 3)
        assertTrue(list1.isEqualCollection(list2))
    }

    @Test
    fun testIsEqualCollection_DifferentDuplicateCounts() {
        val list1 = listOf(1, 2, 2, 3)
        val list2 = listOf(1, 2, 3, 3)
        assertFalse(list1.isEqualCollection(list2))
    }

    @Test
    fun testIsEqualCollection_DifferentSizes() {
        val list1 = listOf(1, 2, 3)
        val list2 = listOf(1, 2, 3, 4)
        assertFalse(list1.isEqualCollection(list2))
    }

    @Test
    fun testIsEqualCollection_DifferentElements() {
        val list1 = listOf(1, 2, 3)
        val list2 = listOf(4, 5, 6)
        assertFalse(list1.isEqualCollection(list2))
    }

    @Test
    fun testIsEqualCollection_EmptyCollections() {
        val list1 = emptyList<Int>()
        val list2 = emptyList<Int>()
        assertTrue(list1.isEqualCollection(list2))
    }

    @Test
    fun testIsEqualCollection_OneEmpty() {
        val list1 = listOf(1, 2, 3)
        val list2 = emptyList<Int>()
        assertFalse(list1.isEqualCollection(list2))
    }

    @Test
    fun testIsEqualCollection_NullOther() {
        val list1 = listOf(1, 2, 3)
        assertFalse(list1.isEqualCollection(null))
    }

    @Test
    fun testIsEqualCollection_WithNulls() {
        val list1 = listOf(1, null, 3)
        val list2 = listOf(1, null, 3)
        assertTrue(list1.isEqualCollection(list2))
    }

    @Test
    fun testIsEqualCollection_OneWithNull() {
        val list1 = listOf(1, null, 3)
        val list2 = listOf(1, 2, 3)
        assertFalse(list1.isEqualCollection(list2))
    }

    @Test
    fun testIsEqualCollection_StringCollection() {
        val list1 = listOf("a", "b", "c")
        val list2 = listOf("c", "b", "a")
        assertTrue(list1.isEqualCollection(list2))
    }

    @Test
    fun testIsEqualCollection_Set() {
        val set1 = setOf(1, 2, 3)
        val set2 = setOf(3, 2, 1)
        assertTrue(set1.isEqualCollection(set2))
    }

    @Test
    fun testIsEqualCollection_MixedTypes() {
        val list1 = listOf(1, "a", 2.5)
        val list2 = listOf(2.5, "a", 1)
        assertTrue(list1.isEqualCollection(list2))
    }

    @Test
    fun testIsEqualCollection_LargeCollections() {
        val list1 = (1..1000).toList()
        val list2 = (1..1000).shuffled()
        assertTrue(list1.isEqualCollection(list2))
    }
}
