package io.kudos.base.tree

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * IdAndNameTreeNode test cases.
 *
 * Covers: primary/secondary constructors, ITreeNode accessors (_getId/_getParentId/_getChildren),
 * compareTo both branches (orderNum present vs. name fallback), the _getId null-bang path,
 * data class equals/copy, and integration with [ListToTreeConverter] sorting.
 *
 * @author K
 * @since 1.0.0
 */
internal class IdAndNameTreeNodeTest {

    @Test
    fun testPrimaryConstructorAndAccessors() {
        val node = IdAndNameTreeNode(id = "1", name = "root", parentId = null, orderNum = 5)
        assertEquals("1", node._getId())
        assertNull(node._getParentId())
        assertTrue(node._getChildren().isEmpty())
        assertEquals(5, node.orderNum)
        assertEquals("root", node.name)
    }

    @Test
    fun testParentIdAccessor() {
        val node = IdAndNameTreeNode(id = "2", name = "child", parentId = "1")
        assertEquals("1", node._getParentId())
    }

    @Test
    fun testSecondaryNoArgConstructorDefaults() {
        val node = IdAndNameTreeNode<String>()
        assertNull(node.id)
        assertEquals("", node.name)
        assertNull(node.parentId)
        assertNull(node.orderNum)
        assertTrue(node._getChildren().isEmpty())
    }

    @Test
    fun testGetIdThrowsWhenIdIsNull() {
        // _getId() uses id!!; a null id (e.g. from the no-arg constructor) triggers NPE
        val node = IdAndNameTreeNode<String>()
        assertFailsWith<NullPointerException> {
            node._getId()
        }
    }

    @Test
    fun testChildrenIsMutableAndShared() {
        val node = IdAndNameTreeNode(id = "1", name = "root")
        val child = IdAndNameTreeNode(id = "2", name = "child", parentId = "1")
        node._getChildren().add(child)
        assertEquals(1, node.children.size)
        assertSame(child, node.children[0])
    }

    @Test
    fun testCompareToUsesOrderNumWhenBothPresent() {
        val a = IdAndNameTreeNode(id = "1", name = "zzz", orderNum = 1)
        val b = IdAndNameTreeNode(id = "2", name = "aaa", orderNum = 2)
        // orderNum wins over name: a (1) < b (2)
        assertTrue(a.compareTo(b) < 0)
        assertTrue(b.compareTo(a) > 0)
        assertEquals(0, a.compareTo(IdAndNameTreeNode(id = "3", name = "qqq", orderNum = 1)))
    }

    @Test
    fun testCompareToFallsBackToNameWhenOrderNumMissing() {
        val a = IdAndNameTreeNode(id = "1", name = "aaa", orderNum = null)
        val b = IdAndNameTreeNode(id = "2", name = "bbb", orderNum = null)
        assertTrue(a.compareTo(b) < 0, "with no orderNum, name decides")
        assertTrue(b.compareTo(a) > 0)
    }

    @Test
    fun testCompareToFallsBackToNameWhenOnlyOneOrderNumPresent() {
        val a = IdAndNameTreeNode(id = "1", name = "aaa", orderNum = 100)
        val b = IdAndNameTreeNode(id = "2", name = "bbb", orderNum = null)
        // other.orderNum is null -> name fallback: "aaa" < "bbb"
        assertTrue(a.compareTo(b) < 0)
    }

    @Test
    fun testDataClassEqualsAndCopy() {
        val a = IdAndNameTreeNode(id = "1", name = "x", parentId = "0", orderNum = 1)
        val b = IdAndNameTreeNode(id = "1", name = "x", parentId = "0", orderNum = 1)
        assertEquals(a, b)
        val copied = a.copy(name = "y")
        assertEquals("y", copied.name)
        assertEquals("1", copied.id)
    }

    /** Minimal node that does NOT override _getChildren, exercising the ITreeNode default body. */
    private class MinimalNode(val nid: String) : ITreeNode<String> {
        override fun _getId(): String = nid
        override fun _getParentId(): String? = null
    }

    @Test
    fun testITreeNodeDefaultGetChildrenReturnsEmptyMutableList() {
        val node: ITreeNode<String> = MinimalNode("1")
        val children = node._getChildren()
        assertTrue(children.isEmpty(), "default _getChildren returns an empty mutable list")
        // the default produces a fresh mutableListOf() per call, so mutating one copy does not persist
        children.add(MinimalNode("2"))
        assertEquals(1, children.size, "the returned list itself is mutable")
        assertTrue(node._getChildren().isEmpty(), "a subsequent call returns a fresh empty list")
    }

    @Test
    fun testSortingViaConverterUsesCompareTo() {
        val nodes = listOf(
            IdAndNameTreeNode(id = "1", name = "n", parentId = null, orderNum = 3),
            IdAndNameTreeNode(id = "2", name = "n", parentId = null, orderNum = 1),
            IdAndNameTreeNode(id = "3", name = "n", parentId = null, orderNum = 2)
        )
        val tree = ListToTreeConverter.convert(nodes, direction = io.kudos.base.query.sort.DirectionEnum.ASC)
        assertEquals(listOf(1, 2, 3), tree.map { it.orderNum })
    }
}
