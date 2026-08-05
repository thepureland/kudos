package io.kudos.ms.sys.common.dict.vo.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * test for SysDictTreeNode
 *
 * Covers default property values and mutable id/code reassignment.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDictTreeNodeTest {

    @Test
    fun defaults() {
        val node = SysDictTreeNode()
        assertEquals("", node.id)
        assertNull(node.code)
    }

    @Test
    fun mutablePropertiesCanBeReassigned() {
        val node = SysDictTreeNode()
        node.id = "n-1"
        node.code = "gender"
        assertEquals("n-1", node.id)
        assertEquals("gender", node.code)
    }

}
