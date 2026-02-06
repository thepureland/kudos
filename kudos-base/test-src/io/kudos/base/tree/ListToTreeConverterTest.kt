package io.kudos.base.tree

import io.kudos.base.query.sort.DirectionEnum
import io.kudos.base.support.ICallback
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * ListToTreeConverter测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class ListToTreeConverterTest {

    @Test
    fun testConvert() {
        val list = listOf(
            TestTreeNode("10", null, "根结点10"),
            TestTreeNode("11", "10", "10的子结点11"),
            TestTreeNode("12", "10", "10的子结点12"),
            TestTreeNode("20", null, "根结点20"),
            TestTreeNode("21", "20", "20的子结点21")
        )
        val treeList = ListToTreeConverter.convert(list)
        var result = treeList.size == 2
        val treeNode10 = treeList[0]
        result = result && "10" == treeNode10.id
        result = result && treeNode10._getChildren().size == 2
        val treeNode11 = treeNode10._getChildren()[0]
        result = result && "11" == treeNode11._getId()
        val treeNode12 = treeNode10._getChildren()[1]
        result = result && "12" == treeNode12._getId()
        val treeNode20 = treeList[1]
        result = result && "20" == treeNode20._getId()
        result = result && treeNode20._getChildren().size == 1
        val treeNode21 = treeNode20._getChildren()[0]
        result = result && "21" == treeNode21._getId()
        assert(result)
    }

    @Test
    fun testConvertSimpleTree() {
        val nodes = listOf(
            TestTreeNode("1", null, "Root1"),
            TestTreeNode("2", null, "Root2"),
            TestTreeNode("3", "1", "Child1"),
            TestTreeNode("4", "1", "Child2")
        )
        val tree = ListToTreeConverter.convert(nodes)
        assertEquals(2, tree.size)
        assertEquals(2, tree[0]._getChildren().size)
    }

    @Test
    fun testConvertWithEmptyList() {
        val nodes = emptyList<TestTreeNode>()
        val tree = ListToTreeConverter.convert(nodes)
        assertTrue(tree.isEmpty())
    }

    @Test
    fun testConvertWithOnlyRoots() {
        val nodes = listOf(
            TestTreeNode("1", null, "Root1"),
            TestTreeNode("2", null, "Root2"),
            TestTreeNode("3", null, "Root3")
        )
        val tree = ListToTreeConverter.convert(nodes)
        assertEquals(3, tree.size)
        tree.forEach { assertTrue(it._getChildren().isEmpty()) }
    }

    @Test
    fun testConvertWithNestedChildren() {
        val nodes = listOf(
            TestTreeNode("1", null, "Root"),
            TestTreeNode("2", "1", "Child1"),
            TestTreeNode("3", "2", "Grandchild1"),
            TestTreeNode("4", "2", "Grandchild2")
        )
        val tree = ListToTreeConverter.convert(nodes)
        assertEquals(1, tree.size)
        assertEquals(1, tree[0]._getChildren().size)
        assertEquals(2, tree[0]._getChildren()[0]._getChildren().size)
    }

    @Test
    fun testConvertWithEmptyStringParentId() {
        val nodes = listOf(
            TestTreeNode("1", "", "Root1"),
            TestTreeNode("2", "", "Root2")
        )
        val tree = ListToTreeConverter.convert(nodes)
        assertEquals(2, tree.size)
    }

    @Test
    fun testConvertWithMissingParent() {
        val nodes = listOf(
            TestTreeNode("1", null, "Root"),
            TestTreeNode("2", "999", "Orphan") // 父节点不存在
        )
        val tree = ListToTreeConverter.convert(nodes)
        assertEquals(1, tree.size)
        // 孤儿节点应该被忽略
        assertTrue(tree[0]._getChildren().isEmpty())
    }

    @Test
    fun testConvertWithCallback() {
        val nodes = listOf(
            TestTreeNode("1", null, "Root1"),
            TestTreeNode("2", null, "Root2")
        )
        var callbackCount = 0
        val callback = ICallback<TestTreeNode, Unit> { p ->
            callbackCount++
            assertNotNull(p)
        }
        val tree = ListToTreeConverter.convert(nodes, callback = callback)
        assertEquals(2, callbackCount)
        assertEquals(2, tree.size)
    }

    @Test
    fun testConvertWithNullCallback() {
        val nodes = listOf(
            TestTreeNode("1", null, "Root1")
        )
        val tree = ListToTreeConverter.convert(nodes, callback = null)
        assertEquals(1, tree.size)
    }

    @Test
    fun testConvertWithSortAsc() {
        val nodes = listOf(
            TestComparableTreeNode("3", null, "C"),
            TestComparableTreeNode("1", null, "A"),
            TestComparableTreeNode("2", null, "B")
        )
        val tree = ListToTreeConverter.convert(nodes, direction = DirectionEnum.ASC)
        assertEquals(3, tree.size)
        assertEquals("A", tree[0].name)
        assertEquals("B", tree[1].name)
        assertEquals("C", tree[2].name)
    }

    @Test
    fun testConvertWithSortDesc() {
        val nodes = listOf(
            TestComparableTreeNode("1", null, "A"),
            TestComparableTreeNode("2", null, "B"),
            TestComparableTreeNode("3", null, "C")
        )
        val tree = ListToTreeConverter.convert(nodes, direction = DirectionEnum.DESC)
        assertEquals(3, tree.size)
        assertEquals("C", tree[0].name)
        assertEquals("B", tree[1].name)
        assertEquals("A", tree[2].name)
    }

    @Test
    fun testConvertWithSortAndChildren() {
        val nodes = listOf(
            TestComparableTreeNode("1", null, "Root"),
            TestComparableTreeNode("2", "1", "B"),
            TestComparableTreeNode("3", "1", "A")
        )
        val tree = ListToTreeConverter.convert(nodes, direction = DirectionEnum.ASC)
        assertEquals(1, tree.size)
        val children = tree[0]._getChildren()
        assertEquals(2, children.size)
        assertEquals("A", (children[0] as TestComparableTreeNode).name)
        assertEquals("B", (children[1] as TestComparableTreeNode).name)
    }

    @Test
    fun testConvertComplexTree() {
        val nodes = listOf(
            TestTreeNode("1", null, "Root1"),
            TestTreeNode("2", null, "Root2"),
            TestTreeNode("3", "1", "Child1-1"),
            TestTreeNode("4", "1", "Child1-2"),
            TestTreeNode("5", "2", "Child2-1"),
            TestTreeNode("6", "3", "Grandchild1-1")
        )
        val tree = ListToTreeConverter.convert(nodes)
        assertEquals(2, tree.size)
        assertEquals(2, tree[0]._getChildren().size)
        assertEquals(1, tree[1]._getChildren().size)
        assertEquals(1, tree[0]._getChildren()[0]._getChildren().size)
    }

    private class TestComparableTreeNode(
        id: String,
        parentId: String?,
        name: String?
    ) : TestTreeNode(id, parentId, name), Comparable<TestComparableTreeNode> {
        override fun compareTo(other: TestComparableTreeNode): Int {
            return (name ?: "").compareTo(other.name ?: "")
        }
    }
}
