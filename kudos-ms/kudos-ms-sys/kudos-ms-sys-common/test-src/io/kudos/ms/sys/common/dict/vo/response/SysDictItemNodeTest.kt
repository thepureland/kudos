package io.kudos.ms.sys.common.dict.vo.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * test for SysDictItemNode
 *
 * Covers default values, full-args construction and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDictItemNodeTest {

    @Test
    fun defaults() {
        val n = SysDictItemNode(id = "i-1")
        assertEquals("i-1", n.id)
        assertEquals("", n.itemCode)
        assertEquals("", n.itemName)
    }

    @Test
    fun fullArgsConstruction() {
        val n = SysDictItemNode("i-1", "male", "Male")
        assertEquals("i-1", n.id)
        assertEquals("male", n.itemCode)
        assertEquals("Male", n.itemName)
    }

    @Test
    fun dataClassContract() {
        val a = SysDictItemNode("i-1", "male", "Male")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(itemCode = "female"))
        assertTrue(a.toString().contains("male"))
    }

}
