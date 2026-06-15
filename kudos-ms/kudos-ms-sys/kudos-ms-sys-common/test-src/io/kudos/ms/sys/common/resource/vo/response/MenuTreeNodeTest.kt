package io.kudos.ms.sys.common.resource.vo.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for MenuTreeNode
 *
 * Covers its own properties (index / icon) plus inherited BaseMenuTreeNode behavior.
 *
 * @author K
 * @since 1.0.0
 */
internal class MenuTreeNodeTest {

    @Test
    fun defaults() {
        val node = MenuTreeNode()
        assertNull(node.index)
        assertNull(node.icon)
        assertNull(node.title)
        assertNull(node.id)
        assertTrue(node is BaseMenuTreeNode)
    }

    @Test
    fun mutablePropertiesCanBeReassigned() {
        val node = MenuTreeNode()
        node.index = "/home"
        node.icon = "icon-home"
        node.title = "Home"
        node.id = "n-1"
        node.seqNo = 1
        assertEquals("/home", node.index)
        assertEquals("icon-home", node.icon)
        assertEquals("Home", node.title)
        assertEquals("n-1", node.id)
        assertEquals("n-1", node._getId())
    }

    @Test
    fun inheritsCompareTo() {
        val a = MenuTreeNode().apply { seqNo = 1 }
        val b = MenuTreeNode().apply { seqNo = 2 }
        assertTrue(a.compareTo(b) < 0)
    }

}
