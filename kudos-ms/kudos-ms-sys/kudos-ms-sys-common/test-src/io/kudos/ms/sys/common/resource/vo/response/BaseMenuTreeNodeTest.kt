package io.kudos.ms.sys.common.resource.vo.response

import io.kudos.base.model.contract.result.IJsonResult
import io.kudos.base.tree.ITreeNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for BaseMenuTreeNode
 *
 * Covers default property values, mutable reassignment, the ITreeNode accessors
 * (_getId / _getParentId / _getChildren), the _getId null-check failure path,
 * the IJsonResult / ITreeNode / Comparable contracts and compareTo across all branches
 * (both seqNo present, this seqNo null, other seqNo null).
 *
 * @author K
 * @since 1.0.0
 */
internal class BaseMenuTreeNodeTest {

    @Test
    fun defaults() {
        val node = BaseMenuTreeNode()
        assertNull(node.title)
        assertNull(node.id)
        assertNull(node.parentId)
        assertNull(node.seqNo)
        assertTrue(node.children.isEmpty())
        assertTrue(node is IJsonResult)
        assertTrue(node is ITreeNode<*>)
        assertTrue(node is Comparable<*>)
    }

    @Test
    fun mutablePropertiesCanBeReassigned() {
        val node = BaseMenuTreeNode()
        node.title = "Menu"
        node.id = "n-1"
        node.parentId = "p-1"
        node.seqNo = 5
        assertEquals("Menu", node.title)
        assertEquals("n-1", node.id)
        assertEquals("p-1", node.parentId)
        assertEquals(5, node.seqNo)
    }

    @Test
    fun treeNodeAccessors() {
        val parent = BaseMenuTreeNode().apply { id = "p-1" }
        val child = BaseMenuTreeNode().apply { id = "c-1"; parentId = "p-1" }
        parent.children.add(child)

        assertEquals("p-1", parent._getId())
        assertNull(parent._getParentId())
        assertEquals("p-1", child._getParentId())
        assertSame(parent.children, parent._getChildren())
        assertEquals(1, parent._getChildren().size)
    }

    @Test
    fun getIdThrowsWhenIdIsNull() {
        val node = BaseMenuTreeNode()
        val ex = assertFailsWith<IllegalArgumentException> { node._getId() }
        assertTrue(ex.message!!.contains("BaseMenuTreeNode.id must not be null"))
    }

    @Test
    fun compareToBothPresent() {
        val a = BaseMenuTreeNode().apply { seqNo = 1 }
        val b = BaseMenuTreeNode().apply { seqNo = 2 }
        assertTrue(a.compareTo(b) < 0)
        assertTrue(b.compareTo(a) > 0)
        assertEquals(0, a.compareTo(BaseMenuTreeNode().apply { seqNo = 1 }))
    }

    @Test
    fun compareToReturnsZeroWhenThisSeqNoIsNull() {
        val a = BaseMenuTreeNode()
        val b = BaseMenuTreeNode().apply { seqNo = 2 }
        assertEquals(0, a.compareTo(b))
    }

    @Test
    fun compareToReturnsZeroWhenOtherSeqNoIsNull() {
        val a = BaseMenuTreeNode().apply { seqNo = 2 }
        val b = BaseMenuTreeNode()
        assertEquals(0, a.compareTo(b))
    }

}
