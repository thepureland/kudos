package io.kudos.ms.sys.common.resource.vo.response

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for SysResourceTreeRow
 *
 * Covers default (all-null but id) values, full-args including nested children,
 * and data-class contract.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysResourceTreeRowTest {

    @Test
    fun defaults() {
        val r = SysResourceTreeRow()
        assertEquals("", r.id)
        assertNull(r.name)
        assertNull(r.url)
        assertNull(r.resourceTypeDictCode)
        assertNull(r.parentId)
        assertNull(r.orderNum)
        assertNull(r.icon)
        assertNull(r.subSystemCode)
        assertNull(r.remark)
        assertNull(r.active)
        assertNull(r.builtIn)
        assertNull(r.children)
    }

    @Test
    fun fullArgsWithChildren() {
        val child = SysResourceTreeRow(id = "c-1", name = "Child")
        val r = SysResourceTreeRow(
            id = "r-1",
            name = "Root",
            url = "/u",
            resourceTypeDictCode = "1",
            parentId = null,
            orderNum = 1,
            icon = "ic",
            subSystemCode = "sys",
            remark = "r",
            active = true,
            builtIn = false,
            children = mutableListOf(child),
        )
        assertEquals("r-1", r.id)
        assertEquals(1, r.children!!.size)
        assertEquals("c-1", r.children!![0].id)
    }

    @Test
    fun dataClassContract() {
        val a = SysResourceTreeRow(id = "r-1", name = "Root")
        val b = a.copy()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, a.copy(name = "other"))
        assertTrue(a.toString().contains("r-1"))
    }

}
